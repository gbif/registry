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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.registry.DatabaseInitializer;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.domain.doi.DoiType;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class DoiMapperIT {

  @Autowired private DoiMapper mapper;

  @ClassRule public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

  @Test
  public void testCreate() {
    DOI doi = new DOI("10.998/dead.moon");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DOWNLOAD);
    DoiData data = mapper.get(doi);
    assertNotNull(data);
    assertEquals(DoiStatus.NEW, data.getStatus());
    assertNull(data.getTarget());
  }

  @Test
  public void testList() {
    List<Map<String, Object>> dataBefore = mapper.list(null, DoiType.DATASET, null);

    DOI doi = new DOI("10.998/dead.pool");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DATASET);
    List<Map<String, Object>> data = mapper.list(null, DoiType.DATASET, null);

    assertNotNull(data);
    assertEquals(dataBefore.size() + 1, data.size());
    // assertNull(data.getTarget());
  }

  @Test
  public void testUpdate() {
    DOI doi = new DOI("10.998/dead.kennedys");
    mapper.create(doi, DoiType.DOWNLOAD);

    mapper.update(doi, new DoiData(DoiStatus.NEW, null), null);
    DoiData data = mapper.get(doi);
    assertEquals(DoiStatus.NEW, data.getStatus());
    assertNull(data.getTarget());

    mapper.update(doi, new DoiData(DoiStatus.RESERVED, null), null);
    data = mapper.get(doi);
    assertEquals(DoiStatus.RESERVED, data.getStatus());
    assertNull(data.getTarget());

    URI uri = URI.create("ftp://ftp.bands.com");
    mapper.update(doi, new DoiData(DoiStatus.REGISTERED, uri), null);
    data = mapper.get(doi);
    assertEquals(DoiStatus.REGISTERED, data.getStatus());
    assertEquals(uri, data.getTarget());
  }

  @Test
  public void testDelete() {
    DOI doi = new DOI("10.998/dead.kennedys");
    mapper.create(doi, DoiType.DOWNLOAD);
    assertNotNull(mapper.get(doi));
    mapper.delete(doi);
    assertNull(mapper.get(doi));
  }
}
