/*
 * Class NativeUtils is published under the The MIT License:
 *
 * Copyright (c) 2012 Adam Heinrich <adam@adamh.cz>
 * Modified for use in jvm-brotli by Dominik Petrovic (https://nixxcode.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.nixxcode.jvmbrotli.common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * A simple library class which helps with loading dynamic libraries stored in the
 * JAR archive. These libraries usually contain implementation of some methods in
 * native code (using JNI - Java Native Interface).
 * 
 * @see <a href="http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar">http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar</a>
 * @see <a href="https://github.com/adamheinrich/native-utils">https://github.com/adamheinrich/native-utils</a>
 *
 */
class NativeUtils {
 
    /**
     * The minimum length a prefix for a file has to have according to {@link File#createTempFile(String, String)}}.
     */
    private static final int MIN_FILE_PREFIX_LENGTH = 3;

    /**
     * Private constructor - this class will never be instanced
     */
    private NativeUtils() {
    }

    /**
     * Loads library from current JAR archive
     * 
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after
     * exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     * 
     * @param path The path of file inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation fails
     * @throws IllegalArgumentException If source file (param path) does not exist
     * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters
     * (restriction of {@link File#createTempFile(java.lang.String, java.lang.String)}).
     * @throws FileNotFoundException If the file could not be found inside the JAR.
     */
    public static void loadLibraryFromJar(String tempDirPrefix, String path) throws IOException {

        // Check if the temp dir prefix is okay
        if (tempDirPrefix == null || tempDirPrefix.length() < MIN_FILE_PREFIX_LENGTH) {
            throw new IllegalArgumentException("The temp dir prefix has to be at least 3 characters long.");
        }

        if (null == path || !path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }
 
        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
 
        // Check if the filename is okay
        if (filename == null || filename.length() < MIN_FILE_PREFIX_LENGTH) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }

        File tempDir = getOrCreateTempDirectory(tempDirPrefix, filename);
        File tempFile = new File(tempDir, filename);

        // Replace the file if it exists. This ensures we are never loading an old version of the lib
        try (InputStream is = NativeUtils.class.getResourceAsStream(path)) {
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tempFile.delete();
            throw e;
        } catch (NullPointerException e) {
            tempFile.delete();
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }

        try {
            System.load(tempFile.getAbsolutePath());
        } catch (Throwable t) {
            throw new IOException(t.getMessage());
        }
    }

    private static File getOrCreateTempDirectory(String tempDirPrefix, String filename) throws IOException {
        String[] fileNameParts = filename.split(Pattern.quote("."));
        String fileNameNoExtension = fileNameParts[0];

        String implVersion = NativeUtils.class.getPackage().getImplementationVersion();
        implVersion = implVersion != null ? implVersion : "null.version";
        String implVersionNoPeriods = implVersion.replace(".", "-");

        // Build temp directory name
        String generatedDirName = tempDirPrefix + "-"
                + implVersionNoPeriods + "-"
                + fileNameNoExtension;

        String systemTempDir = System.getProperty("java.io.tmpdir");
        File tempDir = new File(systemTempDir, generatedDirName);

        // Create dir if it doesn't already exist
        if (!tempDir.exists()) {
            if (!tempDir.mkdir()) {
                throw new IOException("Failed to create temp directory " + tempDir.getName());
            }
        }

        return tempDir;
    }
}
