package org.brotli;/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.brotli.wrapper.dec.BrotliInputStream;
import org.brotli.wrapper.enc.BrotliOutputStream;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/** Tests for {@link BrotliInputStream}. */
@RunWith(AllTests.class)
public class BrotliInputStreamTest extends BrotliJniTestBase {

  static InputStream getBundle() throws IOException {
    Class clazz = BrotliOutputStreamTest.class;
    return clazz.getResourceAsStream("/file/test_data.zip");
  }

  @Before
  static void loadLib() {
    URL resource = BrotliOutputStream.class.getResource("/brotli.dll");
    Runtime.getRuntime().load(resource.getFile());
  }

  /** Creates a test suite. */
  public static TestSuite suite() throws IOException {
    loadLib();
    TestSuite suite = new TestSuite();
    InputStream bundle = getBundle();
    try {
      List<String> entries = BundleHelper.listEntries(bundle);
      for (String entry : entries) {
        suite.addTest(new StreamTestCase(entry));
      }
    } finally {
      bundle.close();
    }
    return suite;
  }

  /** Test case with a unique name. */
  static class StreamTestCase extends TestCase {
    final String entryName;
    StreamTestCase(String entryName) {
      super("org.brotli.BrotliInputStreamTest." + entryName);
      this.entryName = entryName;
    }

    @Override
    protected void runTest() throws Throwable {
      BrotliInputStreamTest.run(entryName);
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

    InputStream src = new ByteArrayInputStream(compressed);
    InputStream decoder = new BrotliInputStream(src);
    long crc;
    try {
      crc = BundleHelper.fingerprintStream(decoder);
    } finally {
      decoder.close();
    }
    assertEquals(BundleHelper.getExpectedFingerprint(entryName), crc);
  }
}
