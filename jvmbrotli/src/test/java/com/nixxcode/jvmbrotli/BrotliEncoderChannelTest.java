package com.nixxcode.jvmbrotli;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliEncoderChannel;
import com.nixxcode.jvmbrotli.enc.Encoder;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/** Tests for {@link com.nixxcode.jvmbrotli.enc.BrotliEncoderChannel}. */
@RunWith(AllTests.class)
public class BrotliEncoderChannelTest extends BrotliJniTestBase {

  private enum TestMode {
    WRITE_ALL,
    WRITE_CHUNKS
  }

  private static final int CHUNK_SIZE = 256;

  static void loadLib() {
    BrotliLoader.loadBrotli();
  }

  static InputStream getBundle() throws IOException {
    Class clazz = BrotliOutputStreamTest.class;
    return clazz.getResourceAsStream("/file/test_corpus.zip");
  }

  /** Creates a test suite. */
  public static TestSuite suite() throws IOException {
    loadLib();
    TestSuite suite = new TestSuite();
    InputStream bundle = getBundle();
    try {
      List<String> entries = BundleHelper.listEntries(bundle);
      for (String entry : entries) {
        suite.addTest(new ChannleTestCase(entry, TestMode.WRITE_ALL));
        suite.addTest(new ChannleTestCase(entry, TestMode.WRITE_CHUNKS));
      }
    } finally {
      bundle.close();
    }
    return suite;
  }

  /** Test case with a unique name. */
  static class ChannleTestCase extends TestCase {
    final String entryName;
    final TestMode mode;
    ChannleTestCase(String entryName, TestMode mode) {
      super("BrotliEncoderChannelTest." + entryName + "." + mode.name());
      this.entryName = entryName;
      this.mode = mode;
    }

    @Override
    protected void runTest() throws Throwable {
      BrotliEncoderChannelTest.run(entryName, mode);
    }
  }

  private static void run(String entryName, TestMode mode) throws Throwable {
    InputStream bundle = getBundle();
    byte[] original;
    try {
      original = BundleHelper.readEntry(bundle, entryName);
    } finally {
      bundle.close();
    }
    if (original == null) {
      throw new RuntimeException("Can't read bundle entry: " + entryName);
    }

    if ((mode == TestMode.WRITE_CHUNKS) && (original.length <= CHUNK_SIZE)) {
      return;
    }

    ByteArrayOutputStream dst = new ByteArrayOutputStream();
    WritableByteChannel encoder = new BrotliEncoderChannel(Channels.newChannel(dst), new Encoder.Parameters().setQuality(2));
    ByteBuffer src = ByteBuffer.wrap(original);
    try {
      switch (mode) {
        case WRITE_ALL:
          encoder.write(src);
          break;

        case WRITE_CHUNKS:
          while (src.hasRemaining()) {
            int limit = Math.min(CHUNK_SIZE, src.remaining());
            ByteBuffer slice = src.slice();
            ((Buffer) slice).limit(limit);
            ((Buffer) src).position(src.position() + limit);
            encoder.write(slice);
          }
          break;
      }
    } finally {
      encoder.close();
    }

    InputStream decoder = new BrotliInputStream(new ByteArrayInputStream(dst.toByteArray()));
    try {
      long originalCrc = BundleHelper.fingerprintStream(new ByteArrayInputStream(original));
      long crc = BundleHelper.fingerprintStream(decoder);
      assertEquals(originalCrc, crc);
    } finally {
      decoder.close();
    }
  }
}
