package com.nixxcode.jvmbrotli.examples;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class UsageExample {

    public UsageExample() {}

    /**
     * @param filePath Full path to existing file
     */
    public static void compressFile(String filePath) {
        try {
            // Init file input and output
            FileInputStream inFile = new FileInputStream(filePath);
            FileOutputStream outFile = new FileOutputStream(filePath + ".br");

            // If being used to compress streams in real-time, I do not advise a quality setting above 4 due to performance
            Encoder.Parameters params = new Encoder.Parameters().setQuality(4);

            // Initialize compressor by binding it to our file output stream
            BrotliOutputStream brotliOutputStream = new BrotliOutputStream(outFile, params);

            int read = inFile.read();
            while(read > -1) { // -1 means EOF
                brotliOutputStream.write(read);
                read = inFile.read();
            }

            // It's important to close the BrotliOutputStream object. This also closes the underlying FileOutputStream
            brotliOutputStream.close();
            inFile.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param filePath Full path to existing file
     */
    public static void decompressFile(String filePath) {
        try {
            // Open input file and obtain critical name components
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String path = FilenameUtils.getPath(filePath);
            String fileName = FilenameUtils.getBaseName(filePath);
            String fileExtension = FilenameUtils.getExtension(filePath);

            if(!fileExtension.equalsIgnoreCase("br")) {
                throw new IllegalArgumentException("File name does not end with .br, not a valid Brotli file.");
            }

            // Create subdirectory so we're not overwriting the original file when decompressing
            File subDir = getOrCreateSubDirectory(path, "decompressed");
            File outFile = new File(subDir, fileName);

            // Initialize decompressor by binding it to our file input stream
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            BrotliInputStream brotliInputStream = new BrotliInputStream(fileInputStream);

            int read = brotliInputStream.read();
            while(read > -1) { // -1 means EOF
                fileOutputStream.write(read);
                read = brotliInputStream.read();
            }

            // It's important to close the BrotliInputStream object. This also closes the underlying FileInputStream
            brotliInputStream.close();
            fileOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    private static File getOrCreateSubDirectory(String basePath, String subDirName) throws IOException {
        File outFolder = new File(basePath, subDirName);
        if(!outFolder.exists()) { // Try to create output subdirectory if it doesn't exist
            if (!outFolder.mkdir()) {
                throw new IOException("Failed to create temp directory " + outFolder.getName());
            }
        }
        return outFolder;
    }

    public static void main(String[] args) {
        BrotliLoader.loadBrotli();

        String plainFile = UsageExample.class.getResource("/file/README.md").getFile();
        compressFile(plainFile);

        String compressedFile = UsageExample.class.getResource("/file/README.md.br").getFile();
        decompressFile(compressedFile);
    }
}
