# Jvm-Brotli: Lightweight, cross-platform Java library for the Brotli compression format

Jvm-Brotli's goal is to increase the availability of the Brotli compression algorithm for Java-based frameworks. It aims to:

- Take the original c/c++ and Java code from [google/brotli](https://github.com/google/brotli), and keep it as close as possible to the original. (only make minor performance tweaks where needed)
- Compile it on multiple platforms
- Package the pre-compiled native libraries into JARs and make them available via maven central
- Provide source code with project breakdown and build instructions, to make forking and building as easy as possible for platforms that are not yet supported.

#### Motivation
When searching through the existing set of Brotli solutions for Java, I found [meteogroup/jbrotli](https://github.com/MeteoGroup/jbrotli) and thought it was a fantastic approach towards improving Brotli availability for Java developers everywhere.

Since jBrotli is now several years out of date, I decided to start my own solution based on the same objective: Making Brotli accessible to Java developers on as many platforms as possible

## General Information

* Project website: (coming soon)
* Documentation: (coming soon)
* Chat: (coming soon)
* Licensing: (coming soon)

## Supported Platforms

* Windows (32 and 64 bit) - Tested on Windows 7 and 10
* Linux (64 bit) - Tested on Ubuntu 18.04
* Linux on ARM processors (64 bit) - UNTESTED
* Mac OSX - Tested on Mojave

## Getting Started

Jvm-Brotli is designed with ease of use in mind. To include it in your Maven project, simply add the following dependency to your pom.xml

```
<dependency>
    <groupId>com.nixxcode.jvmbrotli</groupId>
    <artifactId>jvmbrotli</artifactId>
    <version>0.1.0</version>
</dependency>
```
That's all! You don't need to worry about what platform you're on, as long as it's supported. The correct native library will be downloaded automatically as an additional Maven dependency.

If your platform is not supported, but you still want to use Jvm-Brotli right now, please scroll down to the "Build Instructions" section. 

We would also really appreciate it if you could create an issue and request support for your platform. Remember, the goal of this project is to make Java Brotli available on as many platforms as possible. We can make it happen with your help!

### Usage Examples

Fully functional example code  can be found [here](https://github.com/nixxcode/jvm-brotli/tree/release-prep/jvmbrotli/src/test/java/com/nixxcode/jvmbrotli/examples)

#### Encoding a stream:
```
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

```

#### Decoding a stream:
```
// Init file input and output
FileInputStream inFile = new FileInputStream(filePath);
FileOutputStream outFile = new FileOutputStream(decodedfilePath);

// Initialize decompressor by binding it to our file input stream
BrotliInputStream brotliInputStream = new BrotliInputStream(inFile);

int read = brotliInputStream.read();
while(read > -1) { // -1 means EOF
    outFile.write(read);
    read = brotliInputStream.read();
}

// It's important to close the BrotliInputStream object. This also closes the underlying FileInputStream
brotliInputStream.close();
outFile.close();
```

## Project Structure

Coming soon

## Build Instructions

Coming soon

## License

Google Brotli and corresponding Java code are licensed under the [MIT License](https://opensource.org/licenses/MIT)

Code belonging to the repository author (Nixxcode) and MeteoGroup Deutschland GmbH is licensed under the [Apache License](https://www.apache.org/licenses/LICENSE-2.0)

If in doubt, refer to individual source file headers

## Thanks

- [Google Brotli](https://github.com/google/brotli) team for creating Brotli.
- [Martin W. Kirst](https://github.com/nitram509) for creating the original jBrotli that inspired this project.
- [David Åse](https://github.com/tipsy) for his help and guidance in publishing my first OS project.