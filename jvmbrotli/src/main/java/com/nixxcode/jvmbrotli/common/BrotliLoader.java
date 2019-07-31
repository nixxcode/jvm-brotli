package com.nixxcode.jvmbrotli.common;

import java.io.IOException;
import java.util.Locale;

import static com.nixxcode.jvmbrotli.common.Arch.*;
import static com.nixxcode.jvmbrotli.common.OS.*;

public class BrotliLoader {

    private static final String LIBNAME = "brotli";
    private static final String DIR_PREFIX = "jvmbrotli";

    /**
     * Have we already loaded the native library? Used to avoid multiple load attempts in the same JVM instance
     */
    private static boolean libLoaded = false;

    public static void loadBrotli() {
        if(libLoaded) return;
        try {
            System.loadLibrary(LIBNAME);
            libLoaded = true;
        } catch (UnsatisfiedLinkError linkError) {
            try {
                String nativeLibName = System.mapLibraryName(LIBNAME);
                String libPath = "/lib/" + determineOsArchName() + "/" + nativeLibName;
                NativeUtils.loadLibraryFromJar(DIR_PREFIX, libPath);
                libLoaded = true;
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }

    private static String determineOsArchName() {
        return determineOS() + "-" + determineArch();
    }

    private static String determineOS() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        if (LINUX.matches(osName)) return LINUX.name;
        if (WIN32.matches(osName)) return WIN32.name;
        if (OSX.matches(osName)) return OSX.name;
        return osName;
    }

    private static String determineArch() {
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.US);
        if (X86_AMD64.matches(osArch)) return X86_AMD64.name;
        if (X86.matches(osArch)) return X86.name;
        if (ARM32_VFP_HFLT.matches(osArch)) return ARM32_VFP_HFLT.name;
        return osArch;
    }
}
