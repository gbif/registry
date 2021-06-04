package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;

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
      ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollAuthorizationService grSciCollAuthorizationService
  ) {
    super(
        changeSuggestionMapper,
        institutionMergeService,
        institutionService,
        Institution.class,
        objectMapper,
        emailSender,
        emailManager,
        eventManager, grSciCollAuthorizationService);
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
}
