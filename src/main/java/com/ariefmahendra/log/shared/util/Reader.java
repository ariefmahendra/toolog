package com.ariefmahendra.log.shared.util;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class Reader {

    @Getter
    private static String resultLog = "";

    private static final Logger logger = LoggerFactory.getLogger(Reader.class);

    public static void readFileByCapacity(int capacity, String pathDownloaded) {
        logger.info("Starting Read");
        try (RandomAccessFile raf = new RandomAccessFile(pathDownloaded, "r")) {
            raf.seek(raf.length() - capacity);
            byte[] fileReaderByCapacity = new byte[capacity];
            raf.read(fileReaderByCapacity);
            resultLog = new String(fileReaderByCapacity, StandardCharsets.UTF_8);
            logger.info("End Read");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
