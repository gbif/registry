/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.cli.common;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.io.Resources;

import static org.junit.Assert.assertEquals;

public class SingleColumnFileReaderTest {

  @Test
  public void testReadFile() throws IOException {
    URL fileUrl = Resources.getResource("ids.txt");
    List<Integer> idAsInt =
        SingleColumnFileReader.readFile(
            fileUrl.getPath(),
            new Function<String, Integer>() {
              @Nullable
              @Override
              public Integer apply(String input) {
                return Integer.parseInt(input);
              }
            });
    assertEquals(3, idAsInt.size());
  }
}
