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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.UUID;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link InstitutionService}. */
public class InstitutionServiceIT extends BaseCollectionEntityServiceIT<Institution> {

  private final InstitutionService institutionService;

  @Autowired
  public InstitutionServiceIT(
      InstitutionService institutionService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      InstitutionDuplicatesService duplicatesService) {
    super(
        institutionService,
        datasetService,
        nodeService,
        organizationService,
        installationService,
        principalProvider,
        duplicatesService,
        Institution.class);
    this.institutionService = institutionService;
  }

  @Execution(ExecutionMode.CONCURRENT)
  @Test
  public void createInstitutionWithoutCodeTest() {
    Institution i = testData.newEntity();
    i.setCode(null);
    assertThrows(ValidationException.class, () -> institutionService.create(i));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @Test
  public void updateInstitutionWithoutCodeTest() {
    Institution i = testData.newEntity();
    UUID key = institutionService.create(i);

    Institution created = institutionService.get(key);
    created.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @Test
  public void updateAndReplaceTest() {
    Institution i = testData.newEntity();
    UUID key = institutionService.create(i);

    Institution created = institutionService.get(key);
    created.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));

    created.setReplacedBy(null);
    created.setConvertedToCollection(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @Test
  public void possibleDuplicatesTest() {
    testDuplicatesCommonCases();
  }

  @Execution(ExecutionMode.CONCURRENT)
  @Test
  public void invalidEmailsTest() {
    Institution institution = new Institution();
    institution.setCode("cc");
    institution.setName("n1");
    institution.setEmail(Collections.singletonList("asfs"));

    assertThrows(ConstraintViolationException.class, () -> institutionService.create(institution));

    institution.setEmail(Collections.singletonList("aa@aa.com"));
    UUID key = institutionService.create(institution);
    Institution institutionCreated = institutionService.get(key);

    institutionCreated.getEmail().add("asfs");
    assertThrows(
        ConstraintViolationException.class, () -> institutionService.update(institutionCreated));
  }
}
