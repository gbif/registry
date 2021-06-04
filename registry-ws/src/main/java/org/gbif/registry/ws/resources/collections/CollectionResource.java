/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionSearchParams;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.registry.ws.export.CsvWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@Validated
@RestController
@RequestMapping(value = "grscicoll/collection", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionResource extends ExtendedCollectionEntityResource<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;
  private final DuplicatesService duplicatesService;

  //Prefix for the export file format
  private final static String EXPORT_FILE_PRE = "collections.";

  //Page size to iterate over download stats export service
  private static final int EXPORT_LIMIT = 5_000;

  public CollectionResource(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      EventManager eventManager,
      CollectionMergeService collectionMergeService,
      DuplicatesService duplicatesService,
      WithMyBatis withMyBatis) {
    super(
        collectionMapper,
        addressMapper,
        tagMapper,
        identifierMapper,
        collectionMapper,
        machineTagMapper,
        commentMapper,
        occurrenceMappingMapper,
        collectionMapper,
        collectionMergeService,
        eventManager,
        Collection.class,
        withMyBatis);
    this.collectionMapper = collectionMapper;
    this.duplicatesService = duplicatesService;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/collection/{key}")
  @Override
  public CollectionView getCollectionView(@PathVariable UUID key) {
    CollectionDto collectionDto = collectionMapper.getCollectionDto(key);

    if (collectionDto == null) {
      return null;
    }

    return convertToCollectionView(collectionDto);
  }

  @GetMapping
  @Override
  public PagingResponse<CollectionView> list(CollectionSearchRequest searchRequest) {
    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    CollectionSearchParams params =
        CollectionSearchParams.builder()
            .institutionKey(searchRequest.getInstitution())
            .contactKey(searchRequest.getContact())
            .query(query)
            .code(searchRequest.getCode())
            .name(searchRequest.getName())
            .alternativeCode(searchRequest.getAlternativeCode())
            .machineTagNamespace(searchRequest.getMachineTagNamespace())
            .machineTagName(searchRequest.getMachineTagName())
            .machineTagValue(searchRequest.getMachineTagValue())
            .identifierType(searchRequest.getIdentifierType())
            .identifier(searchRequest.getIdentifier())
            .country(searchRequest.getCountry())
            .city(searchRequest.getCity())
            .fuzzyName(searchRequest.getFuzzyName())
            .build();

    long total = collectionMapper.count(params);
    List<CollectionDto> collectionDtos = collectionMapper.list(params, page);

    List<CollectionView> views =
        collectionDtos.stream().map(this::convertToCollectionView).collect(Collectors.toList());

    return new PagingResponse<>(page, total, views);
  }

  @GetMapping("export")
  public void export(HttpServletResponse response,
                     @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
                     CollectionSearchRequest searchRequest) throws IOException {
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, EXPORT_FILE_PRE + format.name().toLowerCase());

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.collections(Iterables.collections(searchRequest, this, EXPORT_LIMIT), format)
        .export(writer);
    }
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<CollectionView> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;

    long total = collectionMapper.countDeleted();
    List<CollectionDto> dtos = collectionMapper.deleted(page);
    List<CollectionView> views =
        dtos.stream().map(this::convertToCollectionView).collect(Collectors.toList());

    return new PagingResponse<>(page, total, views);
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return collectionMapper.suggest(q);
  }

  @GetMapping("possibleDuplicates")
  public DuplicatesResult findPossibleDuplicates(DuplicatesRequest request) {
    Preconditions.checkArgument(
        !request.isEmpty(), "At least one param to check the same field is required");

    return duplicatesService.findPossibleDuplicateCollections(
        DuplicatesSearchParams.builder()
            .sameFuzzyName(request.getSameFuzzyName())
            .sameName(request.getSameName())
            .sameCode(request.getSameCode())
            .sameCountry(request.getSameCountry())
            .sameCity(request.getSameCity())
            .inCountries(request.getInCountries())
            .notInCountries(request.getNotInCountries())
            .excludeKeys(request.getExcludeKeys())
            .sameInstitutionKey(request.getSameInstitution())
            .inInstitutions(request.getInInstitutions())
            .notInInstitutions(request.getNotInInstitutions())
            .build());
  }

  private CollectionView convertToCollectionView(CollectionDto dto) {
    CollectionView collectionView = new CollectionView(dto.getCollection());
    collectionView.setInstitutionCode(dto.getInstitutionCode());
    collectionView.setInstitutionName(dto.getInstitutionName());
    return collectionView;
  }
}
