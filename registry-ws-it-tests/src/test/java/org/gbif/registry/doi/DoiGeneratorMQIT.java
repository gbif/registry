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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.doi.generator.DoiGenerator;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.beust.jcommander.internal.Sets;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: 18/04/2020 doubt it's useful
public class DoiGeneratorMQIT {
  private DoiGenerator generator;

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  @Before
  public void setup(DoiGenerator doiGenerator) {
    this.generator = doiGenerator;
  }

  @Test
  public void testNewDOI() {
    Set<DOI> dois = Sets.newHashSet();
    for (int x = 1; x < 20; x++) {
      DOI doi = generator.newDatasetDOI();
      assertTrue(generator.isGbif(doi));
      dois.add(doi);
      assertEquals(DOI.TEST_PREFIX, doi.getPrefix());
      assertEquals(x, dois.size());
    }
  }
}
