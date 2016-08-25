package org.gbif.registry.cli.common;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class SingleColumnFileReaderTest {

  @Test
  public void testReadFile() throws IOException {
    List<Integer> idAsInt = SingleColumnFileReader.readFile("ids.txt", new Function<String, Integer>() {
      @Nullable
      @Override
      public Integer apply(String input) {
        return Integer.parseInt(input);
      }
    });
    assertEquals(3, idAsInt.size());
  }
}
