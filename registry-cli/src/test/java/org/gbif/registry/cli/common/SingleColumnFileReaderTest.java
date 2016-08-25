package org.gbif.registry.cli.common;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.io.Resources;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class SingleColumnFileReaderTest {

  @Test
  public void testReadFile() throws IOException {
    URL fileUrl = Resources.getResource("ids.txt");
    List<Integer> idAsInt = SingleColumnFileReader.readFile(fileUrl.getPath(), new Function<String, Integer>() {
      @Nullable
      @Override
      public Integer apply(String input) {
        return Integer.parseInt(input);
      }
    });
    assertEquals(3, idAsInt.size());
  }
}
