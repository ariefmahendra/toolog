package com.ariefmahendra.log.shared.util;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import com.jcraft.jsch.*;
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

    public static void downloadFileByThread(String filePathServer, String pathDownloaded) throws ConnectionException, GeneralException {
        Session session = null;

        logger.info("Starting download file");
        try {
            session = connectSftp();

                ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                sftpChannel.connect();

                // Get file size
                long remoteFileSize = sftpChannel.lstat(filePathServer).getSize();
                File localFile = new File(pathDownloaded);
                long localFileSize = localFile.exists() ? localFile.length() : 0;

                if (localFileSize == remoteFileSize) {
                    logger.info("File already downloaded, skipping download.");
                    return;
                }

                // Calculate chunk size
                long chunkSize = remoteFileSize / THREAD_COUNT;

                if (localFileSize > remoteFileSize) {
                    boolean isDeleted = localFile.delete();
                    if (!isDeleted) {
                        throw new GeneralException("Failed to delete local file");
                    }
                    localFileSize = 0;
                }

                ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
                List<Future<Void>> futures = new ArrayList<>();

                for (int i = 0; i < THREAD_COUNT; i++) {
                    final int threadIndex = i;
                    Future<Void> future = executor.submit(() -> {
                        long startOffset = threadIndex * chunkSize;
                        long endOffset = (threadIndex == (THREAD_COUNT - 1)) ? remoteFileSize : (startOffset + chunkSize);

                        try {
                            downloadChunk(startOffset, endOffset, pathDownloaded + ".part" + threadIndex, filePathServer);
                        } catch (Exception e) {
                            logger.error("Failed to download chunk: {}", threadIndex, e);
                            throw new RuntimeException(e);
                        }
                        return null;
                    });

                    futures.add(future);
                }

                for (Future<Void> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        throw new GeneralException("Error during file download");
                    }
                }

                executor.shutdown();

                try (FileOutputStream fos = new FileOutputStream(pathDownloaded)) {
                    for (int i = 0; i < THREAD_COUNT; i++) {
                        File chunkFile = new File(pathDownloaded + ".part" + i);
                        try (RandomAccessFile raf = new RandomAccessFile(chunkFile, "r")) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = raf.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }
                        boolean deleted = chunkFile.delete();
                        if (!deleted) {
                            logger.warn("Failed to delete chunk file: {}.part{}", pathDownloaded, i);
                        }
                    }
                }

                logger.info("File downloaded successfully: {}", pathDownloaded);
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to SFTP server error", e);
        } catch (JSchException e) {
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException("Connection Error", e);
        } catch (Exception e) {
            logger.error("Failed to download the file", e);
            throw new GeneralException("Error during file download");
        } finally {
            if (session != null) {
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
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated) {
                    throw new RuntimeException("Failed to create directories: " + parentDir.getAbsolutePath());
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
                logger.error("Failed to interact with SFTP server during chunk download", e);
                throw new ConnectionException("Error during chunk download", e);
            } catch (IOException e) {
                logger.error("Failed to read/write during chunk download", e);
                throw new RuntimeException("Error reading/writing chunk", e);
            } finally {
                sftpChannel.disconnect();
            }
        } catch (Exception e) {
            throw new GeneralException("Error during chunk download: " + e.getMessage());
        }
    }

    private static Session connectSftp() throws JSchException {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        String username = credentials.getSftp().getUsername();
        String remoteHost = credentials.getSftp().getRemoteHost();
        String password = credentials.getSftp().getPassword();
        int port = Integer.parseInt(credentials.getSftp().getPort());

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, remoteHost, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking","no");
        session.connect();
        return session;
    }
}
