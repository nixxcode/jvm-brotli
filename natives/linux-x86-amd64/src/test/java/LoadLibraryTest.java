import org.junit.Test;

import java.net.URL;

public class LoadLibraryTest {

  @Test
  public void the_library_can_be_loaded() throws Exception {
    URL resource = this.getClass().getResource("/lib/linux-x86-amd64/libbrotli.so");
    Runtime.getRuntime().load(resource.getFile());
  }
}
