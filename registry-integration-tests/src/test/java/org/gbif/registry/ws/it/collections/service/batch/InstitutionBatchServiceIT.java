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
package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.service.collections.batch.InstitutionBatchService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstitutionBatchServiceIT extends BaseBatchServiceIT<Institution> {

  private final InstitutionService institutionService;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  @Autowired
  public InstitutionBatchServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionService institutionService,
      InstitutionBatchService batchService) {
    super(
        simplePrincipalProvider,
        institutionService,
        batchService,
        CollectionEntityType.INSTITUTION);
    this.institutionService = institutionService;
  }

  @Override
  List listAllEntities() {
    return institutionService.list(InstitutionSearchRequest.builder().build()).getResults();
  }

  @Override
  Institution newInstance() {
    return new Institution();
  }

  @Override
  void assertUpdatedEntity(Institution updated) {
    assertEquals(1, updated.getTypes().size());
    assertEquals(InstitutionType.HERBARIUM, updated.getTypes().get(0));
    assertEquals(2, updated.getDisciplines().size());
  }
}
