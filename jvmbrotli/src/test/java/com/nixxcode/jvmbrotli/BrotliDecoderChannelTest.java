/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/

package com.nixxcode.jvmbrotli;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliDecoderChannel;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/** Tests for {@link com.nixxcode.jvmbrotli.dec.BrotliDecoderChannel}. */
@RunWith(AllTests.class)
public class BrotliDecoderChannelTest extends BrotliJniTestBase {

  static void loadLib() {
    BrotliLoader.loadBrotli();
  }

  static InputStream getBundle() throws IOException {
    Class clazz = BrotliOutputStreamTest.class;
    return clazz.getResourceAsStream("/file/test_data.zip");
  }

  /** Creates a test suite. */
  public static TestSuite suite() throws IOException {
    loadLib();
    TestSuite suite = new TestSuite();
    InputStream bundle = getBundle();
    try {
      List<String> entries = BundleHelper.listEntries(bundle);
      for (String entry : entries) {
        suite.addTest(new ChannelTestCase(entry));
      }
    } finally {
      bundle.close();
    }
    return suite;
  }

  /** Test case with a unique name. */
  static class ChannelTestCase extends TestCase {
    final String entryName;
    ChannelTestCase(String entryName) {
      super("BrotliDecoderChannelTest." + entryName);
      this.entryName = entryName;
    }

    @Override
    protected void runTest() throws Throwable {
      BrotliDecoderChannelTest.run(entryName);
    }
  }

  private static void run(String entryName) throws Throwable {
    InputStream bundle = getBundle();
    byte[] compressed;
    try {
      compressed = BundleHelper.readEntry(bundle, entryName);
    } finally {
      bundle.close();
    }
    if (compressed == null) {
      throw new RuntimeException("Can't read bundle entry: " + entryName);
    }

    ReadableByteChannel src = Channels.newChannel(new ByteArrayInputStream(compressed));
    ReadableByteChannel decoder = new BrotliDecoderChannel(src);
    long crc;
    try {
      crc = BundleHelper.fingerprintStream(Channels.newInputStream(decoder));
    } finally {
      decoder.close();
    }
    assertEquals(BundleHelper.getExpectedFingerprint(entryName), crc);
  }
}
