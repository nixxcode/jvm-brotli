/*
 *  Copyright (c) 2019 Dominik Petrovic (Nixxcode)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.nixxcode.jvmbrotli.common;

import java.io.IOException;
import java.util.Locale;

import static com.nixxcode.jvmbrotli.common.Arch.*;
import static com.nixxcode.jvmbrotli.common.OS.*;

/**
 * Helper class that handles the loading of our native lib
 */
public class BrotliLoader {

    /**
     * Base name of the Brotli library as compiled by CMake. This constant should NOT be changed.
     *
     * This is morphed according to OS by using System.mapLibraryName(). So for example:
     * Windows: brotli.dll
     * Linux:   libbrotli.so
     * Mac:     libbrotli.dylib
     */
    private static final String LIBNAME = "brotli";

    /**
     * Name of directory we create in the system temp folder when unpacking and loading the native library
     *
     * Must be at least 3 characters long, and should be unique to prevent clashing with existing folders in temp
     */
    private static final String DIR_PREFIX = "jvmbrotli";

    /**
     * Have we already loaded the native library? Used to avoid multiple load attempts in the same JVM instance
     */
    private static boolean libLoaded = false;

    /**
     * No sense trying to load again if we failed the first time
     */
    private static boolean loadAttempted = false;

    public static boolean isBrotliAvailable() {
        if(loadAttempted) {
            return libLoaded;
        }
        else {
            try {
                loadBrotli();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                return libLoaded;
            }
        }
    }

    @Deprecated // Use isBrotliAvailable() instead. Will be made private in version 1.0.0.
    public static void loadBrotli() {
        if(loadAttempted) return;
        try { // Try system lib path first
            System.loadLibrary(LIBNAME);
            libLoaded = true;
        } catch (UnsatisfiedLinkError linkError) { // If system load fails, attempt to unpack from jar and then load
            try {
                String nativeLibName = System.mapLibraryName(LIBNAME);
                String libPath = "/lib/" + determineOsArchName() + "/" + nativeLibName;
                NativeUtils.loadLibraryFromJar(DIR_PREFIX, libPath);
                libLoaded = true;
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        } finally {
            loadAttempted = true;
        }
    }

    private static String determineOsArchName() {
        String os = determineOS();
        String arch = determineArch();
        if(os == null) throw new RuntimeException("Unsupported operating system");
        if(arch == null) throw new RuntimeException("Unsupported architecture");
        return os + "-" + arch;
    }

    private static String determineOS() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        if (LINUX.matches(osName)) return LINUX.name;
        if (WIN32.matches(osName)) return WIN32.name;
        if (OSX.matches(osName)) return OSX.name;
        return null;
    }

    private static String determineArch() {
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.US);
        if (X86_AMD64.matches(osArch)) return X86_AMD64.name;
        if (X86.matches(osArch)) return X86.name;
        if (ARM32_VFP_HFLT.matches(osArch)) return ARM32_VFP_HFLT.name;
        return null;
    }
}
