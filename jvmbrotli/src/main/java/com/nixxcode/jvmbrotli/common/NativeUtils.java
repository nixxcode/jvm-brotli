/*
 * Class NativeUtils is published under the The MIT License:
 *
 * Copyright (c) 2012 Adam Heinrich <adam@adamh.cz>
 * Modified for use in jvm-brotli by Dominik Petrovic (http://nixxcode.com)
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
import java.nio.file.*;
import java.util.regex.Pattern;

/**
 * A simple library class which helps with loading dynamic libraries stored in the
 * JAR archive. These libraries usually contain implementation of some methods in
 * native code (using JNI - Java Native Interface).
 * 
 * @see <a href="http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar">http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar</a>
 * @see <a href="https://github.com/adamheinrich/native-utils">https://github.com/adamheinrich/native-utils</a>
 *
 * MODIFICATION: I modified Adam's original code to find and delete unused library folders from previous runs.
 * The modified code runs at the end of the loadLibraryFromJar method. It creats a lock file to "protect"
 * our newly created temp lib while it's in use. It then runs a new method that finds any folders starting with
 * the dirPrefix argument, checks for the presence of the lock file, then deletes the folder and all its contents
 * if the lock file does not exist.
 * Since the lock file itself is deleted when the process exits, this automatically flags the folder as
 * safe to delete for our custom garbage cleaning routine.
 *
 * The reason for the mod, is the original code was flooding the temp directory on Windows with a new folder and a new
 * copy of the target library for each execution. This is due to a known problem with Java still holding a lock on the
 * Windows .dll file when deleteOnExit() is executed. This could potentially get out of hand very fast if this class
 * is called repeatedly, in rapid succession (e.g. test execution).
 *
 * This version still attempts to clean up after itself where possible, but in the event of problems such as the above,
 * subsequent runs should clean up any mess left behind by their predecessors.
 *
 * WARNING: It is YOUR responsibility as the caller, to provide the loadLibraryFromJar method with a unique tempDirPrefix.
 * This is important, since the cleanUnusedCopies method uses this prefix to identify (and delete) unused directories
 * we created on previous runs.
 */
public class NativeUtils {

    /**
     * The minimum length a prefix for a file has to have according to {@link File#createTempFile(String, String)}}.
     */
    private static final int MIN_PREFIX_LENGTH = 3;

    /**
     * Temporary directory which will contain the DLLs.
     */
    private static File temporaryDir;

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
     * @param dirPrefix Prefix name for our folder created inside of the system temp directory.
     *                  Should be something unique to your application, as it is used to create (and identify)
     *                  temporary directories belonging to it. Min length: 3 characters
     * @param path The path of file inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation fails
     * @throws IllegalArgumentException If source file (param path) does not exist
     * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters
     * (restriction of {@link File#createTempFile(java.lang.String, java.lang.String)}).
     * @throws FileNotFoundException If the file could not be found inside the JAR.
     */
    public static void loadLibraryFromJar(String dirPrefix, String path) throws IOException {

        if(dirPrefix.length() < MIN_PREFIX_LENGTH) {
            throw new IllegalArgumentException("The temp dir prefix has to be at least 3 characters long.");
        }

        if (null == path || !path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }

        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

        // Check if the filename is okay
        if (filename == null || filename.length() < MIN_PREFIX_LENGTH) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }

        // Prepare temporary file
        if (temporaryDir == null) {
            temporaryDir = createTempDirectory(dirPrefix);
            temporaryDir.deleteOnExit();
        }

        File temp = new File(temporaryDir, filename);

        try (InputStream is = NativeUtils.class.getResourceAsStream(path)) {
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            temp.delete();
            throw e;
        } catch (NullPointerException e) {
            temp.delete();
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }

        try {
            System.load(temp.getAbsolutePath());
        } finally {
            if (isPosixCompliant()) {
                // Assume POSIX compliant file system, can be deleted after loading
                temp.delete();
            } else {
                // Assume non-POSIX, and don't delete until last file descriptor closed
                temp.deleteOnExit();
            }
        }

        // create lock file
        final File lock = new File( temp.getAbsolutePath() + ".lock");
        lock.createNewFile();
        lock.deleteOnExit();

        cleanUnusedCopies(dirPrefix, filename);
    }



    private static boolean isPosixCompliant() {
        try {
            return FileSystems.getDefault()
                    .supportedFileAttributeViews()
                    .contains("posix");
        } catch (FileSystemNotFoundException
                | ProviderNotFoundException
                | SecurityException e) {
            return false;
        }
    }

    private static File createTempDirectory(String prefix) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());

        if (!generatedDir.mkdir())
            throw new IOException("Failed to create temp directory " + generatedDir.getName());

        return generatedDir;
    }

    private static void cleanUnusedCopies(String dirPrefix, String fileName) {
        // Find dir names starting with our prefix
        FileFilter tmpDirFilter = pathname -> pathname.getName().startsWith(dirPrefix);

        // Get all folders from system temp dir that match our filter
        String tmpDirName = System.getProperty("java.io.tmpdir");
        File[] tmpDirs = new File(tmpDirName).listFiles(tmpDirFilter);

        for (File tDir : tmpDirs) {
            // Create a file to represent the lock and test.
            File lockFile = new File( tDir.getAbsolutePath() + "/" + fileName + ".lock");

            // If lock file doesn't exist, it means this directory and lib file are no longer in use, so delete them
            if (!lockFile.exists()) {
                File[] tmpFiles = tDir.listFiles();
                for(File tFile : tmpFiles) {
                    tFile.delete();
                }
                tDir.delete();
            }
        }
    }
}