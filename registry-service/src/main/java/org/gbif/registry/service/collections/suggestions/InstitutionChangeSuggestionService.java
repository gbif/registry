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
package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkArgument;

@Service
@Validated
public class InstitutionChangeSuggestionService
    extends BaseChangeSuggestionService<Institution, InstitutionChangeSuggestion> {

  private static final Logger LOG =
      LoggerFactory.getLogger(InstitutionChangeSuggestionService.class);

  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final InstitutionMergeService institutionMergeService;

  @Autowired
  public InstitutionChangeSuggestionService(
      ChangeSuggestionMapper changeSuggestionMapper,
      InstitutionService institutionService,
      InstitutionMergeService institutionMergeService,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollAuthorizationService grSciCollAuthorizationService,
      CollectionsMailConfigurationProperties collectionsMailConfigurationProperties) {
    super(
        changeSuggestionMapper,
        institutionMergeService,
        institutionService,
        institutionService,
        Institution.class,
        objectMapper,
        emailSender,
        emailManager,
        eventManager,
        grSciCollAuthorizationService,
        collectionsMailConfigurationProperties);
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.institutionMergeService = institutionMergeService;
  }

  @Override
  protected ChangeSuggestionDto createConvertToCollectionSuggestionDto(
      InstitutionChangeSuggestion institutionChangeSuggestion) {
    checkArgument(institutionChangeSuggestion.getEntityKey() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(institutionChangeSuggestion);
    dto.setInstitutionConvertedCollection(
        institutionChangeSuggestion.getInstitutionForConvertedCollection());
    dto.setNameNewInstitutionConvertedCollection(
        institutionChangeSuggestion.getNameForNewInstitutionForConvertedCollection());

    return dto;
  }

  @Override
  public InstitutionChangeSuggestion getChangeSuggestion(int key) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(key);

    if (dto == null || dto.getEntityType() != CollectionEntityType.INSTITUTION) {
      return null;
    }

    InstitutionChangeSuggestion suggestion = dtoToChangeSuggestion(dto);
    suggestion.setInstitutionForConvertedCollection(dto.getInstitutionConvertedCollection());
    suggestion.setNameForNewInstitutionForConvertedCollection(
        dto.getNameNewInstitutionConvertedCollection());

    return suggestion;
  }

  @Override
  protected UUID applyConversionToCollection(ChangeSuggestionDto dto) {
    return institutionMergeService.convertToCollection(
        dto.getEntityKey(),
        dto.getInstitutionConvertedCollection(),
        dto.getNameNewInstitutionConvertedCollection());
  }

  @Override
  protected InstitutionChangeSuggestion newEmptyChangeSuggestion() {
    return new InstitutionChangeSuggestion();
  }

  public void fix() {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(1);

    ObjectMapper om = new ObjectMapper();

    try {
      JsonNode rootSugg = om.readTree(dto.getSuggestedEntity());
      if (rootSugg.has("foundingDate")) {
        LocalDateTime date = LocalDateTime.parse(rootSugg.path("foundingDate").asText());
        ((ObjectNode) rootSugg).put("foundingDate", date.getYear());
        dto.setSuggestedEntity(om.writeValueAsString(rootSugg));

        dto.getChanges().stream()
          .filter(c -> c.getFieldName().equals("foundingDate"))
          .forEach(c -> {
            c.setFieldType(Integer.class);
            c.setSuggested(date.getYear());
          });
      }

      changeSuggestionMapper.update(dto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
