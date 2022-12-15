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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DoiMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer(CONTAINER, "gbif_doi");

  private DoiMapper mapper;

  @Autowired
  public DoiMapperIT(
      DoiMapper doiMapper, SimplePrincipalProvider principalProvider, EsManageServer esServer) {
    super(principalProvider, esServer);
    this.mapper = doiMapper;
  }

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
    DOI doi = new DOI("10.998/dead.pool");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DATASET);
    List<Map<String, Object>> data = mapper.list(null, DoiType.DATASET, null);

    assertNotNull(data);
    assertEquals(1, data.size());
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
