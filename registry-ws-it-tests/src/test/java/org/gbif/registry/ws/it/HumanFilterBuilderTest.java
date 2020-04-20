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
package org.gbif.registry.ws.it;

import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.Country;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HumanFilterBuilderTest {
  // make sure this is the right bundle as used by the human query builder
  private static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("org/gbif/occurrence/query/filter");

  /**
   * Test all available search parameters and make sure we have a bundle entry for all possible enum
   * values. Test copied from download-query-tools, but added here again as we might have a
   * different GBIF API version in use.
   */
  @Test
  public void testEnumResourceBundle() throws Exception {
    for (OccurrenceSearchParameter p : OccurrenceSearchParameter.values()) {
      if (p.type().isEnum()) {
        if (p.type() != Country.class && p.type() != Continent.class) {
          Class<Enum<?>> vocab = (Class<Enum<?>>) p.type();
          // make sure we have en entry for all possible enum values
          for (Enum<?> e : vocab.getEnumConstants()) {
            assertTrue( resourceBundle.containsKey("enum." + vocab.getSimpleName().toLowerCase() + "." + e.name()),
                        "Missing enum resource bundle entry for " + vocab.getSimpleName() + "." + e.name());
          }
        }
      }
    }
  }
}
