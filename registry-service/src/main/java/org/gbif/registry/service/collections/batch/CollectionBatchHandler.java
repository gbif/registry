package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.persistence.mapper.BatchMapper;
import org.gbif.registry.service.collections.batch.FileFields.InstitutionFields;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static org.gbif.registry.service.collections.batch.FileParser.ParsingData;

// TODO: interface in gbif-api
public class CollectionBatchHandler extends BaseBatchHandler<Collection> {

  private final CollectionService collectionService;

  // TODO: check required columns??

  @Autowired
  public CollectionBatchHandler(BatchMapper batchMapper, CollectionService collectionService) {
    super(batchMapper, collectionService, CollectionEntityType.COLLECTION, Collection.class);
    this.collectionService = collectionService;
  }

  @Override
  List<String> getEntityFields() {
    List<String> fields = new ArrayList<>(InstitutionFields.ALL_FIELDS);
    fields.addAll(FileFields.CommonFields.ALL_FIELDS);
    return fields;
  }

  @Override
  ParsingData<Collection> createEntityFromValues(
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
