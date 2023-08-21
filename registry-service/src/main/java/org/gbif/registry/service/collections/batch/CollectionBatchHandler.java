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
package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.batch.model.ParsedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

@Service
public class CollectionBatchHandler extends BaseBatchHandler<Collection> {

  private final CollectionService collectionService;
  private final GrSciCollAuthorizationService authorizationService;

  @Autowired
  public CollectionBatchHandler(
      BatchMapper batchMapper,
      CollectionService collectionService,
      GrSciCollAuthorizationService authorizationService,
      @Value("${grscicoll.batchResultPath}") String resultPath) {
    super(
        batchMapper,
        collectionService,
        resultPath,
        CollectionEntityType.COLLECTION,
        Collection.class);
    this.collectionService = collectionService;
    this.authorizationService = authorizationService;
  }

  @Override
  boolean allowedToCreateEntity(Collection entity, Authentication authentication) {
    return authorizationService.allowedToCreateCollection(entity, authentication);
  }

  @Override
  boolean allowedToUpdateEntity(Collection entity, Authentication authentication) {
    return authorizationService.allowedToModifyCollection(authentication, entity.getKey(), entity);
  }

  @Override
  List<String> getEntityFields() {
    List<String> fields = new ArrayList<>(FileFields.CollectionFields.ALL_FIELDS);
    fields.addAll(FileFields.CommonFields.ALL_FIELDS);
    return fields;
  }

  @Override
  ParsedData<Collection> createEntityFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    return FileParser.createCollectionFromValues(values, headersIndex);
  }

  @Override
  List<UUID> findEntity(String code, List<Identifier> identifiers) {
    List<CollectionView> collectionsFound = new ArrayList<>();
    if (!Strings.isNullOrEmpty(code)) {
      collectionsFound =
          collectionService.list(CollectionSearchRequest.builder().code(code).build()).getResults();

      if (collectionsFound.isEmpty()) {
        collectionsFound =
            collectionService
                .list(CollectionSearchRequest.builder().alternativeCode(code).build())
                .getResults();
      }
    }

    if (collectionsFound.isEmpty() && identifiers != null && !identifiers.isEmpty()) {
      int i = 0;
      while (i < identifiers.size() && collectionsFound.isEmpty()) {
        Identifier identifier = identifiers.get(i);
        collectionsFound =
            collectionService
                .list(
                    CollectionSearchRequest.builder()
                        .identifier(identifier.getIdentifier())
                        .identifierType(identifier.getType())
                        .build())
                .getResults();
        i++;
      }
    }

    return !collectionsFound.isEmpty()
        ? collectionsFound.stream()
            .map(c -> c.getCollection().getKey())
            .collect(Collectors.toList())
        : new ArrayList<>();
  }
}
