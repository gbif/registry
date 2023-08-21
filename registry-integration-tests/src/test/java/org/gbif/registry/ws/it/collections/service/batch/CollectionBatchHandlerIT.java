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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.service.collections.batch.CollectionBatchHandler;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.springframework.beans.factory.annotation.Autowired;

public class CollectionBatchHandlerIT extends BaseBatchHandlerIT<Collection> {

  @Autowired
  public CollectionBatchHandlerIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionService collectionService,
      CollectionBatchHandler collectionBatchHandler) {
    super(
        simplePrincipalProvider,
        collectionBatchHandler,
        collectionService,
        CollectionEntityType.COLLECTION);
  }

  @Override
  Collection newEntity() {
    return new Collection();
  }
}
