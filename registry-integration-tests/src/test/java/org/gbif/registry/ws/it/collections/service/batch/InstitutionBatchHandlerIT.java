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
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.service.collections.batch.InstitutionBatchHandler;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.springframework.beans.factory.annotation.Autowired;

public class InstitutionBatchHandlerIT extends BaseBatchHandlerIT<Institution> {

  @Autowired
  public InstitutionBatchHandlerIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionService institutionService,
      InstitutionBatchHandler institutionBatchHandler) {
    super(
        simplePrincipalProvider,
        institutionBatchHandler,
        institutionService,
        CollectionEntityType.INSTITUTION);
  }

  @Override
  Institution newEntity() {
    return new Institution();
  }
}
