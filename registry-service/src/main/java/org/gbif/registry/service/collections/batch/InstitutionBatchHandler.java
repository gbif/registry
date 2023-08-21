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

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.batch.FileFields.InstitutionFields;
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
public class InstitutionBatchHandler extends BaseBatchHandler<Institution> {
  private final InstitutionService institutionService;
  private final GrSciCollAuthorizationService authorizationService;

  @Autowired
  public InstitutionBatchHandler(
      BatchMapper batchMapper,
      InstitutionService institutionService,
      GrSciCollAuthorizationService authorizationService,
      @Value("${grscicoll.batchResultPath}") String resultPath) {
    super(
        batchMapper,
        institutionService,
        resultPath,
        CollectionEntityType.INSTITUTION,
        Institution.class);
    this.institutionService = institutionService;
    this.authorizationService = authorizationService;
  }

  @Override
  boolean allowedToCreateEntity(Institution entity, Authentication authentication) {
    return authorizationService.allowedToCreateInstitution(entity, authentication);
  }

  @Override
  boolean allowedToUpdateEntity(Institution entity, Authentication authentication) {
    return authorizationService.allowedToModifyInstitution(authentication, entity.getKey());
  }

  @Override
  List<String> getEntityFields() {
    List<String> fields = new ArrayList<>(InstitutionFields.ALL_FIELDS);
    fields.addAll(FileFields.CommonFields.ALL_FIELDS);
    return fields;
  }

  @Override
  ParsedData<Institution> createEntityFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    return FileParser.createInstitutionFromValues(values, headersIndex);
  }

  @Override
  List<UUID> findEntity(String code, List<Identifier> identifiers) {
    List<Institution> institutionsFound = new ArrayList<>();
    if (!Strings.isNullOrEmpty(code)) {
      institutionsFound =
          institutionService
              .list(InstitutionSearchRequest.builder().code(code).build())
              .getResults();

      if (institutionsFound.isEmpty()) {
        institutionsFound =
            institutionService
                .list(InstitutionSearchRequest.builder().alternativeCode(code).build())
                .getResults();
      }
    }

    if (institutionsFound.isEmpty() && identifiers != null && !identifiers.isEmpty()) {
      int i = 0;
      while (i < identifiers.size() && institutionsFound.isEmpty()) {
        Identifier identifier = identifiers.get(i);
        institutionsFound =
            institutionService
                .list(
                    InstitutionSearchRequest.builder()
                        .identifier(identifier.getIdentifier())
                        .identifierType(identifier.getType())
                        .build())
                .getResults();
        i++;
      }
    }

    return !institutionsFound.isEmpty()
        ? institutionsFound.stream().map(Institution::getKey).collect(Collectors.toList())
        : new ArrayList<>();
  }
}
