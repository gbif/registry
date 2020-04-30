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

import org.gbif.api.model.common.DOI;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ConstantConditions")
public class SingleColumnFileReaderTest {

  @Test
  public void testReadFileIds() {
    // given
    String filePath = ClassLoader.getSystemClassLoader().getResource("ids.txt").getPath();
    Function<String, Integer> toIntegerFunction = Integer::valueOf;

    // when
    List<Integer> idAsInt = SingleColumnFileReader.readFile(filePath, toIntegerFunction);

    // then
    assertEquals(3, idAsInt.size());
  }

  @Test
  public void testReadFileUuids() {
    // given
    String filePath = ClassLoader.getSystemClassLoader().getResource("uuids.txt").getPath();

    // when
    List<UUID> idAsInt = SingleColumnFileReader.readFile(filePath, SingleColumnFileReader::toUuid);

    // then
    // in total 6 lines, but one is invalid UUID
    assertEquals(5, idAsInt.size());
  }

  @Test
  public void testReadFileDois() {
    // given
    String filePath = ClassLoader.getSystemClassLoader().getResource("dois.txt").getPath();

    // when
    List<DOI> idAsInt = SingleColumnFileReader.readFile(filePath, SingleColumnFileReader::toDoi);

    // then
    // in total 5 lines, but one is invalid DOI
    assertEquals(4, idAsInt.size());
  }
}
