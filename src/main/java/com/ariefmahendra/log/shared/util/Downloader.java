package com.ariefmahendra.log.shared.util;

import com.ariefmahendra.log.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Downloader {

    private static final int THREAD_COUNT = 6;
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Downloader.class);

    public static void downloadFileByThread(String filePathServer, String pathDownloaded) throws ConnectionException {
        ChannelSftp sftpChannel = null;
        Session session = null;

        logger.info("Starting download file");
        try {
            session = connectSftp();
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // get file size
            long remoteFileSize = sftpChannel.lstat(filePathServer).getSize();
            File localFile = new File(pathDownloaded);
            long localFileSize = localFile.exists() ? localFile.length() : 0;

            if (localFileSize == remoteFileSize) {
                logger.info("File already downloaded, Skipping download");
                return;
            }

            // calculate chunk size
            long chunkSize = remoteFileSize / THREAD_COUNT;

            if (remoteFileSize < localFileSize){
                boolean isDeleted = localFile.delete();
                if (!isDeleted) {
                   throw new GeneralException("Failed to delete local file");
                }
            }

            // executor service to manage file
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<Void>> futures = new ArrayList<>();

            // schedule task for each thread
            for (int i = 0; i < THREAD_COUNT; i++){
                final int threadIndex = i;
                Future<Void> future = executor.submit(() -> {
                    long startOffset = 0;
                    long endOffset = 0;

                    if (remoteFileSize > localFileSize) {
                        startOffset = localFileSize + (threadIndex * chunkSize);
                        endOffset = (threadIndex == (THREAD_COUNT - 1)) ? remoteFileSize : (startOffset + chunkSize);
                    }

                    if (remoteFileSize < localFileSize) {
                        startOffset =(threadIndex * chunkSize);
                        endOffset = (threadIndex == (THREAD_COUNT - 1)) ? remoteFileSize : (startOffset + chunkSize);
                    }

                    // download chunk
                    Downloader.downloadChunk(startOffset, endOffset, pathDownloaded + ".part" + threadIndex, filePathServer);
                    return null;
                });

                futures.add(future);
            }

            // wait for all thread to complete
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (Exception e){
                    throw new Exception(e);
                }
            }

            executor.shutdown();


            // merge chunks into the final file
            try (FileOutputStream fos = new FileOutputStream(pathDownloaded, true)) {
                for (int i = 0; i < THREAD_COUNT; i++){
                    try (RandomAccessFile raf = new RandomAccessFile(pathDownloaded + ".part" + i, "r")) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = raf.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }

            // delete chunks
            for (int i = 0; i < THREAD_COUNT; i++){
                boolean delete = new File(pathDownloaded + ".part" + i).delete();
                if (!delete) {
                    logger.info("Failed to delete file: {}.part{}", pathDownloaded, i);
                }
            }

            logger.info("File downloaded successfully: {}", pathDownloaded);
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to sftp server error", e);
        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException("Connection Error", e);
        } catch (Exception e) {
            logger.error("Failed to read the log file", e);
            throw new RuntimeException("Error reading log file", e);
        } finally {
            if (sftpChannel != null){
                sftpChannel.disconnect();
            }

            if (session != null){
                session.disconnect();
            }
        }
    }

    private static void downloadChunk(long startOffset, long endOffset, String chunkFilePath, String filePath) throws GeneralException {
        try {
            Session session = connectSftp();
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            File chunkFile = new File(chunkFilePath);
            // Ensure the directory exists
            File parentDir = chunkFile.getParentFile();
            if (!parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated) {
                    throw new RuntimeException("Failed to create directories: " + parentDir.getAbsolutePath());
                }
            }

            // Create the file if it does not exist
            if (!chunkFile.exists()) {
                boolean newFile = chunkFile.createNewFile();
                if (!newFile) {
                    throw new RuntimeException("Failed to create file: " + chunkFilePath);
                }
            }

            try (InputStream inputStream = sftpChannel.get(filePath, null, startOffset);
                 FileOutputStream fos = new FileOutputStream(chunkFilePath)) {

                byte[] buffer = new byte[4096];
                long bytesRead = 0;
                while (bytesRead < (endOffset - startOffset)) {
                    int len = inputStream.read(buffer, 0, (int) Math.min(buffer.length, endOffset - startOffset - bytesRead));
                    if (len == -1) break;
                    fos.write(buffer, 0, len);
                    bytesRead += len;
                }
            } catch (SftpException e) {
                logger.error("Failed to connect or interact with SFTP server", e);
                throw new ConnectionException("Connection to sftp server error", e);
            } catch (IOException e) {
                logger.error("Failed to read the log file", e);
                throw new RuntimeException("Error reading log file", e);
            } finally {
                sftpChannel.disconnect();
                session.disconnect();
            }
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static Session connectSftp() throws JSchException {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();

        return Network.setupJsch(
                credentials.getSftp().getUsername(),
                credentials.getSftp().getRemoteHost(),
                credentials.getSftp().getPassword(),
                Integer.parseInt(credentials.getSftp().getPort())
        );
    }
}
