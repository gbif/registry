package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;
import org.gbif.registry.service.collections.batch.FileFields.InstitutionFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;

import org.springframework.stereotype.Service;

import static org.gbif.registry.service.collections.batch.FileParser.ParsingData;

// TODO: interface in gbif-api
@Service
public class InstitutionBatchService extends BaseBatchService<Institution> {
  private final InstitutionService institutionService;

  // TODO: check required columns??

  @Autowired
  public InstitutionBatchService(BatchMapper batchMapper, InstitutionService institutionService) {
    super(batchMapper, institutionService, CollectionEntityType.INSTITUTION, Institution.class);
    this.institutionService = institutionService;
  }

  @Override
  List<String> getEntityFields() {
    List<String> fields = new ArrayList<>(InstitutionFields.ALL_FIELDS);
    fields.addAll(FileFields.CommonFields.ALL_FIELDS);
    return fields;
  }

  @Override
  ParsingData<Institution> createEntityFromValues(
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
