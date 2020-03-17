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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

/** Utility class that reads a single column from a file. */
public class SingleColumnFileReader {

  private static final Logger LOG = LoggerFactory.getLogger(SingleColumnFileReader.class);

  /**
   * Return a list of all the DOI in a file where they are stored as 1 DOI per line.
   *
   * @param fileName
   * @return
   */
  public static List<DOI> readDOIs(String fileName) {
    List<DOI> doiList = Lists.newArrayList();
    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
      String line;
      DOI doi;
      while ((line = br.readLine()) != null) {
        try {
          doi = new DOI(line.trim());
          doiList.add(doi);
        } catch (IllegalArgumentException e) {
          LOG.error("Ignore invalid DOI: {}", line);
        }
      }
    } catch (FileNotFoundException e) {
      LOG.error("Could not find DOI key file [{}]. Exiting", fileName, e);
    } catch (IOException e) {
      LOG.error("Error while reading DOI key file [{}]. Exiting", fileName, e);
    }
    return doiList;
  }

  /**
   * Reads all (non empty) lines of a file and returns the entire content as List.
   *
   * @param filePath file path
   * @param toType function to transform a String into the expected type
   * @param <T> expected type
   * @return list of content in expected type
   * @throws IOException if file not found or there was a problem reading from file
   */
  public static <T> List<T> readFile(String filePath, final Function<String, T> toType)
      throws IOException {
    Preconditions.checkNotNull(toType);
    Preconditions.checkNotNull(filePath);
    return Resources.readLines(
        Paths.get(filePath).toUri().toURL(),
        StandardCharsets.UTF_8,
        new LineProcessor<List<T>>() {

          List<T> list = Lists.newArrayList();

          @Override
          public boolean processLine(String line) throws IOException {
            T obj = toType.apply(line);
            if (obj != null) {
              list.add(obj);
            }
            return true;
          }

          @Override
          public List<T> getResult() {
            return list;
          }
        });
  }
}
