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
package org.gbif.registry.service.collections.descriptors;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.persistence.mapper.collections.DescriptorsMapper;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.dto.VerbatimDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorGroupParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorParams;
import org.gbif.registry.service.collections.batch.FileParsingUtils;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.rest.client.species.NameUsageMatchingService;
import org.gbif.vocabulary.client.ConceptClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.api.util.GrSciCollUtils.*;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseDateRangeParameters;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseIntegerRangeParameters;

@Validated
@Service
@Slf4j
public class DefaultDescriptorService implements DescriptorsService {

  private final NameUsageMatchingService nameUsageMatchingService;
  private final DescriptorsMapper descriptorsMapper;
  private final EventManager eventManager;
  private final CollectionService collectionService;
  private final ConceptClient conceptClient;

  @Autowired
  public DefaultDescriptorService(
      NameUsageMatchingService nameUsageMatchingService,
      DescriptorsMapper descriptorsMapper,
      EventManager eventManager,
      CollectionService collectionService,
      ConceptClient conceptClient) {
    this.nameUsageMatchingService = nameUsageMatchingService;
    this.descriptorsMapper = descriptorsMapper;
    this.eventManager = eventManager;
    this.collectionService = collectionService;
    this.conceptClient = conceptClient;
  }

  @SneakyThrows
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public long createDescriptorGroup(
      @NotNull @Valid byte[] descriptorGroupFile,
      @NotNull ExportFormat format,
      @NotNull String title,
      String description,
      Set<String> tags,
      @NotNull UUID collectionKey) {
    Objects.requireNonNull(descriptorGroupFile);
    Preconditions.checkArgument(descriptorGroupFile.length > 0);
    Objects.requireNonNull(collectionKey);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(title));

    // Validate tags against vocabulary server
    Vocabularies.checkDescriptorGroupTags(conceptClient, tags);

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setTitle(title);
    descriptorGroup.setDescription(description);
    descriptorGroup.setCreatedBy(username);
    descriptorGroup.setModifiedBy(username);
    descriptorGroup.setCollectionKey(collectionKey);
    if (tags != null && !tags.isEmpty()) {
      descriptorGroup.setTags(tags);
    }
    descriptorsMapper.createDescriptorGroup(descriptorGroup);

    importDescriptorsFile(descriptorGroupFile, format, descriptorGroup.getKey());

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            collectionKey,
            Collection.class,
            DescriptorGroup.class,
            descriptorGroup.getKey(),
            EventType.CREATE));

    return descriptorGroup.getKey();
  }

  private void importDescriptorsFile(
      @NotNull @Valid byte[] descriptorFile, ExportFormat format, long descriptorGroupKey)
      throws IOException {
    // csv options
    CSVParser csvParser = new CSVParserBuilder().withSeparator(format.getDelimiter()).build();

    Map<Integer, String> headersByIndex = new HashMap<>();
    Map<String, Integer> headersByName = new HashMap<>();
    try (CSVReader csvReader =
        new CSVReaderBuilder(
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(descriptorFile))))
            .withCSVParser(csvParser)
            .build()) {
      // extract headers
      String[] headers = csvReader.readNextSilently();
      for (int i = 0; i < headers.length; i++) {
        headersByIndex.put(i, headers[i]);
        headersByName.put(headers[i], i);
      }

      String[] values;
      while ((values = csvReader.readNextSilently()) != null) {
        if (values.length == 0) {
          continue;
        }

        values = FileParsingUtils.normalizeValues(headersByIndex.entrySet().size(), values);

        Map<String, String> valuesMap = valuesAndHeadersToMap(values, headersByName);

        DescriptorDto descriptorDto = interpretDescriptor(valuesMap);
        descriptorDto.setDescriptorGroupKey(descriptorGroupKey);
        descriptorsMapper.createDescriptor(descriptorDto);

        // verbatim fields
        for (int i = 0; i < values.length; i++) {
          descriptorsMapper.createVerbatim(
              descriptorDto.getKey(), headersByIndex.get(i), values[i]);
        }
      }
    }
  }

  private static Map<String, String> valuesAndHeadersToMap(
      String[] values, Map<String, Integer> headersByName) {
    if (values.length == 0) {
      return Collections.emptyMap();
    }

    return headersByName.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> values[e.getValue()]));
  }

  private <T> void setResult(
      DescriptorDto descriptorDto,
      InterpretedResult<T> result,
      BiConsumer<DescriptorDto, T> setter) {
    setter.accept(descriptorDto, result.getResult());
    addIssues(descriptorDto, result);
  }

  private static <T> void addIssues(DescriptorDto descriptorDto, InterpretedResult<T> result) {
    if (descriptorDto.getIssues() == null) {
      descriptorDto.setIssues(new ArrayList<>());
    }
    if (result.getIssues() != null) {
      descriptorDto.getIssues().addAll(result.getIssues());
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void deleteDescriptorGroup(@NotNull long key) {
    DescriptorGroup descriptorGroup = descriptorsMapper.getDescriptorGroup(key);
    Preconditions.checkArgument(
        descriptorGroup != null, "Descriptor group not found for key " + key);

    if (isIHDescriptorGroup(key, descriptorGroup.getCollectionKey())) {
      // can't delete a descriptor group that comes from IH
      return;
    }

    descriptorsMapper.deleteDescriptorGroup(key);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            descriptorGroup.getCollectionKey(),
            Collection.class,
            DescriptorGroup.class,
            key,
            EventType.DELETE));
  }

  @Override
  public DescriptorGroup getDescriptorGroup(@NotNull long key) {
    return descriptorsMapper.getDescriptorGroup(key);
  }

  @SneakyThrows
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void updateDescriptorGroup(
      @NotNull long descriptorGroupKey,
      byte[] descriptorGroupFile,
      @NotNull ExportFormat format,
      @NotNull String title,
      Set<String> tags,
      String description
) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(title));

    // Validate tags against vocabulary server
    Vocabularies.checkDescriptorGroupTags(conceptClient, tags);

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();

    DescriptorGroup descriptorGroup = descriptorsMapper.getDescriptorGroup(descriptorGroupKey);

    if (isIHDescriptorGroup(descriptorGroupKey, descriptorGroup.getCollectionKey())) {
      // can't update a descriptor group that comes from IH
      return;
    }

    descriptorGroup.setTitle(title);
    descriptorGroup.setDescription(description);
    descriptorGroup.setModifiedBy(username);
    if (tags != null) {
      descriptorGroup.setTags(tags);
    }
    descriptorsMapper.updateDescriptorGroup(descriptorGroup);
    if (descriptorGroupFile != null ) {
    // remove descriptors
    descriptorsMapper.deleteDescriptors(descriptorGroup.getKey());

    // reimport the file
    importDescriptorsFile(descriptorGroupFile, format, descriptorGroup.getKey());
    }
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            descriptorGroup.getCollectionKey(),
            Collection.class,
            DescriptorGroup.class,
            descriptorGroupKey,
            EventType.UPDATE));
  }

  private boolean isIHDescriptorGroup(long descriptorGroupKey, UUID collectionKey) {
    Collection collection = collectionService.get(collectionKey);
    List<Long> ihDescriptorGroups =
        collection.getMachineTags().stream()
            .filter(
                mt ->
                    mt.getNamespace().equals(IH_NS)
                        && (mt.getName().equals(COLL_SUMMARY_MT)
                            || mt.getName().equals(COLLECTORS_MT))
                        && mt.getValue() != null)
            .map(mt -> Long.parseLong(mt.getValue()))
            .collect(Collectors.toList());

    return collection.getMasterSource().equals(MasterSourceType.IH)
        && ihDescriptorGroups.contains(descriptorGroupKey);
  }

  @Override
  public PagingResponse<DescriptorGroup> listDescriptorGroups(
      @NotNull UUID collectionKey, DescriptorGroupSearchRequest searchRequest) {
    Objects.requireNonNull(collectionKey);
    if (searchRequest == null) {
      searchRequest = DescriptorGroupSearchRequest.builder().build();
    }

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();
    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    DescriptorGroupParams params =
        DescriptorGroupParams.builder()
            .query(query)
            .collectionKey(collectionKey)
            .title(searchRequest.getTitle())
            .description(searchRequest.getDescription())
            .deleted(searchRequest.getDeleted())
            .tags(searchRequest.getTags())
            .page(page)
            .build();

    return new PagingResponse<>(
        page,
        descriptorsMapper.countDescriptorGroups(params),
        descriptorsMapper.listDescriptorGroups(params));
  }

  @Override
  public Descriptor getDescriptor(@NotNull long key) {
    return convertRecordDto(descriptorsMapper.getDescriptor(key));
  }

  @Override
  public PagingResponse<Descriptor> listDescriptors(DescriptorSearchRequest searchRequest) {
    if (searchRequest == null) {
      searchRequest = DescriptorSearchRequest.builder().build();
    }

    DescriptorParams params = createDescriptorParams(searchRequest);
    List<DescriptorDto> dtos = descriptorsMapper.listDescriptors(params);
    List<Descriptor> results =
        dtos.stream().map(DefaultDescriptorService::convertRecordDto).collect(Collectors.toList());

    return new PagingResponse<>(
        params.getPage(), descriptorsMapper.countDescriptors(params), results);
  }

  private DescriptorParams createDescriptorParams(DescriptorSearchRequest searchRequest) {
    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();
    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    return DescriptorParams.builder()
        .query(query)
        .descriptorGroupKey(searchRequest.getDescriptorGroupKey())
        .country(searchRequest.getCountry())
        .dateIdentified(parseDateRangeParameters(searchRequest.getDateIdentified()))
        .discipline(searchRequest.getDiscipline())
        .individualCount(parseIntegerRangeParameters(searchRequest.getIndividualCount()))
        .usageKey(searchRequest.getUsageKey())
        .usageName(searchRequest.getUsageName())
        .usageRank(searchRequest.getUsageRank())
        .taxonKey(searchRequest.getTaxonKey())
        .objectClassification(searchRequest.getObjectClassification())
        .recordedBy(searchRequest.getRecordedBy())
        .identifiedBy(searchRequest.getIdentifiedBy())
        .issues(searchRequest.getIssues())
        .typeStatus(searchRequest.getTypeStatus())
        .page(page)
        .build();
  }

  @Override
  public long countDescriptors(DescriptorSearchRequest searchRequest) {
    DescriptorParams params = createDescriptorParams(searchRequest);
    return descriptorsMapper.countDescriptors(params);
  }

  @Override
  public Set<String> getVerbatimNames(long descriptorGroupKey) {
    return descriptorsMapper.getVerbatimNames(descriptorGroupKey).stream()
        .sorted(Comparator.comparing(VerbatimDto::getKey))
        .map(VerbatimDto::getFieldName)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void reinterpretDescriptorGroup(long descriptorGroupKey) {
    int limit = 100;
    long offset = 0;
    List<DescriptorDto> descriptorDtos =
        descriptorsMapper.listDescriptors(
            DescriptorParams.builder()
                .descriptorGroupKey(descriptorGroupKey)
                .page(new PagingRequest(offset, limit))
                .build());
    while (!descriptorDtos.isEmpty()) {
      descriptorDtos.forEach(
          dto -> {
            DescriptorDto reinterpretedDto =
                interpretDescriptor(verbatimDtosToMap(dto.getVerbatim()));
            reinterpretedDto.setKey(dto.getKey());
            descriptorsMapper.updateDescriptor(reinterpretedDto);
          });
      offset += limit;
      descriptorDtos =
          descriptorsMapper.listDescriptors(
              DescriptorParams.builder()
                  .descriptorGroupKey(descriptorGroupKey)
                  .page(new PagingRequest(offset, limit))
                  .build());
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void reinterpretCollectionDescriptorGroups(UUID collectionKey) {
    log.info("Starting collection descriptors reinterpretation");
    int limit = 100;
    long offset = 0;
    List<DescriptorGroup> descriptorGroups =
        descriptorsMapper.listDescriptorGroups(
            DescriptorGroupParams.builder()
                .collectionKey(collectionKey)
                .page(new PagingRequest(offset, limit))
                .build());
    while (!descriptorGroups.isEmpty()) {
      descriptorGroups.forEach(dg -> reinterpretDescriptorGroup(dg.getKey()));
      offset += limit;
      descriptorGroups =
          descriptorsMapper.listDescriptorGroups(
              DescriptorGroupParams.builder()
                  .collectionKey(collectionKey)
                  .page(new PagingRequest(offset, limit))
                  .build());
    }
    log.info("Collection descriptors reinterpretation finished");
  }

  @Secured({GRSCICOLL_ADMIN_ROLE})
  @Async
  @Override
  public void reinterpretAllDescriptorGroups() {
    reinterpretCollectionDescriptorGroups(null);
  }

  private DescriptorDto interpretDescriptor(Map<String, String> valuesMap) {
    DescriptorDto descriptorDto = new DescriptorDto();
    // taxonomy
    InterpretedResult<Interpreter.TaxonData> taxonomyResult =
        Interpreter.interpretTaxonomy(valuesMap, nameUsageMatchingService);
    if (taxonomyResult.getResult() != null) {
      descriptorDto.setUsageKey(taxonomyResult.getResult().getUsageKey());
      descriptorDto.setUsageRank(taxonomyResult.getResult().getUsageRank());
      descriptorDto.setUsageName(taxonomyResult.getResult().getUsageName());
      descriptorDto.setTaxonKeys(
          taxonomyResult.getResult().getTaxonKeys() != null
              ? new ArrayList<>(taxonomyResult.getResult().getTaxonKeys())
              : null);
      descriptorDto.setTaxonClassification(taxonomyResult.getResult().getTaxonClassification());
      if (taxonomyResult.getResult().getTaxonClassification() != null) {
        taxonomyResult
            .getResult()
            .getTaxonClassification()
            .forEach(
                r -> {
                  switch (r.getRank().toLowerCase()) {
                    case "kingdom":
                      descriptorDto.setKingdomKey(r.getKey());
                      descriptorDto.setKingdomName(r.getName());
                      break;
                    case "phylum":
                      descriptorDto.setPhylumKey(r.getKey());
                      descriptorDto.setPhylumName(r.getName());
                      break;
                    case "class":
                      descriptorDto.setClassKey(r.getKey());
                      descriptorDto.setClassName(r.getName());
                      break;
                    case "order":
                      descriptorDto.setOrderKey(r.getKey());
                      descriptorDto.setOrderName(r.getName());
                      break;
                    case "family":
                      descriptorDto.setFamilyKey(r.getKey());
                      descriptorDto.setFamilyName(r.getName());
                      break;
                    case "genus":
                      descriptorDto.setGenusKey(r.getKey());
                      descriptorDto.setGenusName(r.getName());
                      break;
                    case "species":
                      descriptorDto.setSpeciesKey(r.getKey());
                      descriptorDto.setSpeciesName(r.getName());
                      break;
                  }
                });
      }
    }
    addIssues(descriptorDto, taxonomyResult);

    // country
    InterpretedResult<Country> countryResult = Interpreter.interpretCountry(valuesMap);
    setResult(descriptorDto, countryResult, DescriptorDto::setCountry);

    // individual count
    InterpretedResult<Integer> individualCountResult =
        Interpreter.interpretIndividualCount(valuesMap);
    setResult(descriptorDto, individualCountResult, DescriptorDto::setIndividualCount);

    // identifiedBy
    InterpretedResult<List<String>> identifiedByResult =
        Interpreter.interpretStringList(valuesMap, DwcTerm.identifiedBy);
    setResult(descriptorDto, identifiedByResult, DescriptorDto::setIdentifiedBy);

    // dateIdentified
    InterpretedResult<Date> dateIdentifiedResult = Interpreter.interpretDateIdentified(valuesMap);
    setResult(descriptorDto, dateIdentifiedResult, DescriptorDto::setDateIdentified);

    // TypeStatus
    InterpretedResult<List<String>> typeStatusResult =
        Interpreter.interpretTypeStatus(valuesMap, conceptClient);
    setResult(descriptorDto, typeStatusResult, DescriptorDto::setTypeStatus);

    // recordedBy
    InterpretedResult<List<String>> recordedByResult =
        Interpreter.interpretStringList(valuesMap, DwcTerm.recordedBy);
    setResult(descriptorDto, recordedByResult, DescriptorDto::setRecordedBy);

    // TODO: create ltc terms??
    // discipline
    InterpretedResult<String> disciplineResult =
        Interpreter.interpretString(valuesMap, "ltc:discipline");
    setResult(descriptorDto, disciplineResult, DescriptorDto::setDiscipline);

    // objectClassification
    InterpretedResult<String> objectClassificationResult =
        Interpreter.interpretString(valuesMap, "ltc:objectClassificationName");
    setResult(
        descriptorDto, objectClassificationResult, DescriptorDto::setObjectClassificationName);

    return descriptorDto;
  }

  private static Descriptor convertRecordDto(DescriptorDto dto) {
    Descriptor descriptorRecord = new Descriptor();
    descriptorRecord.setKey(dto.getKey());
    descriptorRecord.setRecordedBy(dto.getRecordedBy());
    descriptorRecord.setDescriptorGroupKey(dto.getDescriptorGroupKey());
    descriptorRecord.setCountry(dto.getCountry());
    descriptorRecord.setDiscipline(dto.getDiscipline());
    descriptorRecord.setIssues(dto.getIssues());
    descriptorRecord.setDateIdentified(dto.getDateIdentified());
    descriptorRecord.setIdentifiedBy(dto.getIdentifiedBy());
    descriptorRecord.setIndividualCount(dto.getIndividualCount());
    descriptorRecord.setObjectClassification(dto.getObjectClassificationName());
    descriptorRecord.setTypeStatus(dto.getTypeStatus());
    descriptorRecord.setUsageKey(dto.getUsageKey());
    descriptorRecord.setUsageName(dto.getUsageName());
    descriptorRecord.setUsageRank(dto.getUsageRank());
    descriptorRecord.setTaxonClassification(dto.getTaxonClassification());
    descriptorRecord.setVerbatim(verbatimDtosToMap(dto.getVerbatim()));
    return descriptorRecord;
  }

  private static Map<String, String> verbatimDtosToMap(List<VerbatimDto> dtos) {
    return dtos.stream()
        .sorted(Comparator.comparing(VerbatimDto::getKey))
        .collect(
            Collectors.toMap(
                VerbatimDto::getFieldName,
                VerbatimDto::getFieldValue,
                (v1, v2) -> v1,
                LinkedHashMap::new));
  }
}
