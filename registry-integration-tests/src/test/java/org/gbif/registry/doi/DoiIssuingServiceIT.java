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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.beust.jcommander.internal.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ExtendWith(SpringExtension.class)
@Import({
  DoiMessageManagingServiceImpl.class,
  DoiIssuingServiceIT.DoiIssuingServiceITConfiguration.class
})
public class DoiIssuingServiceIT extends BaseItTest {

  private final DoiIssuingService doiIssuingService;

  @Autowired
  public DoiIssuingServiceIT(
      DoiIssuingService doiIssuingService,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.doiIssuingService = doiIssuingService;
  }

  @SpringBootConfiguration
  @MapperScan("org.gbif.registry.persistence.mapper")
  static class DoiIssuingServiceITConfiguration {
    // NOTHING
  }

  @Test
  public void testNewDOI() {
    Set<DOI> dois = new HashSet<>();
    for (int x = 1; x < 20; x++) {
      DOI doi = doiIssuingService.newDatasetDOI();
      assertTrue(doiIssuingService.isGbif(doi));
      dois.add(doi);
      assertEquals(DOI.TEST_PREFIX, doi.getPrefix());
      assertEquals(x, dois.size());
    }
  }
}
