package com.nixxcode.jvmbrotli.common;

import java.io.IOException;

public class BrotliLoader {

    /**
     * Have we already loaded the native library? Used to avoid multiple load attempts in the same JVM instance
     */
    private static boolean libLoaded = false;

    public static void loadBrotli() {
        if(libLoaded) return;
        try {
            System.loadLibrary("brotli");
            libLoaded = true;
        } catch (UnsatisfiedLinkError linkError) {
            try {
                NativeUtils.loadLibraryFromJar("jvmbrotli", "/lib/win32-x86-amd64/brotli.dll");
                libLoaded = true;
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }
}
