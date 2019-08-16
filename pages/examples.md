---
layout: default
title: Examples - Jvm-Brotli
navlogo: true
permalink: /examples
---

<style>{% include landing.css %}</style>

<br/>
# Examples
<br/>
### Loading Jvm-Brotli:
<br/>
You MUST call the following method at least once during your application's runtime:
```java
	BrotliLoader.isBrotliAvailable();
```
This static method call attempts to load the native Brotli library into the current JVM runtime when invoked for the first time. If loading succeeds, it silently returns true. If loading fails, it prints an exception to console and returns false. Subsequent calls only return true/false, depending if the original load attempt was successful or not. No further load attempts are made unless the application is restarted.
<br/>
<br/>
### Encoding a stream:
```java
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
<br/>
<br/>
### Decoding a stream:
```java
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
<br/>
<br/>