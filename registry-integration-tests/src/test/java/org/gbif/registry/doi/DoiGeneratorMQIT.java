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
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.generator.DoiGeneratorMQ;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.beust.jcommander.internal.Nullable;
import com.beust.jcommander.internal.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ExtendWith(SpringExtension.class)
@Import({DoiGeneratorMQ.class, DoiGeneratorMQIT.DoiGeneratorMQITConfiguration.class})
public class DoiGeneratorMQIT extends BaseItTest {

  private DoiGenerator generator;

  @Autowired
  public DoiGeneratorMQIT(
      DoiGenerator generator, @Nullable SimplePrincipalProvider simplePrincipalProvider) {
    super(simplePrincipalProvider);
    this.generator = generator;
  }

  @SpringBootConfiguration
  @MapperScan("org.gbif.registry.persistence.mapper")
  static class DoiGeneratorMQITConfiguration {
    // NOTHING
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
