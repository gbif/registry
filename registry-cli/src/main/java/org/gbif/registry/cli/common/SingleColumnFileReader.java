/*
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class that reads a single column from a file. */
public final class SingleColumnFileReader {

  private static final Logger LOG = LoggerFactory.getLogger(SingleColumnFileReader.class);

  private SingleColumnFileReader() {}

  /** String to DOI function. Logs and ignores wrong DOIs. */
  public static DOI toDoi(String line) {
    try {
      return new DOI(line);
    } catch (IllegalArgumentException e) {
      LOG.error("Ignore invalid DOI: {}", line);
      return null;
    }
  }

  /** String to UUID function. Logs and ignores wrong UUIDs. */
  public static UUID toUuid(String line) {
    try {
      return UUID.fromString(line);
    } catch (IllegalArgumentException e) {
      LOG.error("Key {} is an invalid UUID - skipping!", line);
      return null;
    }
  }

  /**
   * Reads all (non empty) lines of a file and returns the entire content as List.
   *
   * @param filePath file path
   * @param toType function to transform a String into the expected type
   * @param <T> expected type
   * @return list of content in expected type
   */
  public static <T> List<T> readFile(String filePath, final Function<String, T> toType) {
    Objects.requireNonNull(toType);
    Objects.requireNonNull(filePath);

    List<T> result;

    try (Stream<String> lines = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
      result =
          lines
              .map(String::trim)
              .filter(StringUtils::isNotEmpty)
              .map(toType)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
    } catch (FileNotFoundException e) {
      LOG.error("Could not find file [{}]. Exiting", filePath, e);
      result = Collections.emptyList();
    } catch (IOException e) {
      LOG.error("Error while reading file [{}]. Exiting", filePath, e);
      result = Collections.emptyList();
    }

    return result;
  }
}
