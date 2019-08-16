## Project Structure
Jvm-Brotli is a Maven project that contains multiple modules. This sections aims to break down the structure and explain the purpose of the individual modules. 

#### jvmbrotli-parent (pom located in project root)
This is the master or "meta" module for the project. It contains relevant project information, as well as build/publish rules that apply to all submodules.

#### jvmbrotli (pom located in jvmbrotli directory)
This module contains the Java code for the project. It's where the callable jvm-brotli methods live. Most of it is taken directly from the Brotli project on Github, only the native library loader is custom and the tests are modified to make use of said library loader.

#### jvmbrotli-natives (pom located in natives directory)
This is the "master" module for the individual native submodules. It contains profiles which are activated based on the operating system/architecture combination and activate only the os/arch submodule that corresponds to our platform.

The individual native submodules then contain platform-dependent build scripts, which invoke CMake with the CMakeLists file located in the project's root directroy as part of the maven build process.

The idea is to make the native Brotli build process consistent across all platforms, using a universal Maven command.

## Build Instructions
Before building this project, you must have the following pre-requisites installed:
- Java JDK 8+
- CMake v3.0+
- C++ compiler tool chain

The first two are universal. The C++ compiler tool chain will depend on your operating system. There may be multiple choices available, but Jvm-Brotli was built using the following:
- Linux: gcc (for C) and g++ (for C++)
- Mac OSX: AppleClang (included with Xcode)
- Windows: nmake (need to install C++ build tools via Visual Studio installer. You don't need to install the entirety of VS)

Once you have these pre-requisites installed, simply run `mvn package` from the project root.

This will build both the Java and C++ code, and will copy the native library to classpath so it can be loaded and called from Java.

Windows users will likely need to run the usual "vcvarsall.bat" (x86 or x64 depending on platform) in their cmd instance before running the above build command, as this temporarily sets Windows environment variables to allow for C++ compiling via command line. Failure to do this first will likely result in build errors with CMake complaining about missing compilers.
