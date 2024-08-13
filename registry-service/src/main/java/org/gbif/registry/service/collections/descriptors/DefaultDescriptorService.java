package org.gbif.registry.service.collections.descriptors;

import static org.gbif.registry.service.collections.utils.ParamUtils.parseIntegerRangeParameter;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.vocabulary.Country;
import org.gbif.checklistbank.ws.client.NubResourceClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
public class DefaultDescriptorService implements DescriptorsService {

  private final NubResourceClient nubResourceClient;
  private final DescriptorsMapper descriptorsMapper;
  private final EventManager eventManager;

  @Autowired
  public DefaultDescriptorService(
      NubResourceClient nubResourceClient,
      DescriptorsMapper descriptorsMapper,
      EventManager eventManager) {
    this.nubResourceClient = nubResourceClient;
    this.descriptorsMapper = descriptorsMapper;
    this.eventManager = eventManager;
  }

  @SneakyThrows
  @Transactional
  @Override
  public long createDescriptorGroup(
      @NotNull @Valid byte[] descriptorGroupFile,
      @NotNull ExportFormat format,
      @NotNull String title,
      String description,
      @NotNull UUID collectionKey) {
    Objects.requireNonNull(descriptorGroupFile);
    Preconditions.checkArgument(descriptorGroupFile.length > 0);
    Objects.requireNonNull(collectionKey);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(title));

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setTitle(title);
    descriptorGroup.setDescription(description);
    descriptorGroup.setCreatedBy(username);
    descriptorGroup.setModifiedBy(username);
    descriptorGroup.setCollectionKey(collectionKey);
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
        headersByName.put(headers[i].toLowerCase(), i);
      }

      String[] values;
      while ((values = csvReader.readNextSilently()) != null) {
        if (values.length == 0) {
          continue;
        }

        values = FileParsingUtils.normalizeValues(headersByIndex.entrySet().size(), values);

        DescriptorDto descriptorDto = new DescriptorDto();
        descriptorDto.setDescriptorGroupKey(descriptorGroupKey);

        // taxonomy
        InterpretedResult<Interpreter.TaxonData> taxonomyResult =
            Interpreter.interpretTaxonomy(values, headersByName, nubResourceClient);
        if (taxonomyResult.getResult() != null) {
          descriptorDto.setUsageKey(taxonomyResult.getResult().getUsageKey());
          descriptorDto.setUsageRank(taxonomyResult.getResult().getUsageRank());
          descriptorDto.setUsageName(taxonomyResult.getResult().getUsageName());
          descriptorDto.setTaxonKeys(taxonomyResult.getResult().getTaxonKeys());
          descriptorDto.setTaxonClassification(taxonomyResult.getResult().getTaxonClassification());
        }
        addIssues(descriptorDto, taxonomyResult);

        // country
        InterpretedResult<Country> countryResult =
            Interpreter.interpretCountry(values, headersByName);
        setResult(descriptorDto, countryResult, DescriptorDto::setCountry);

        // individual count
        InterpretedResult<Integer> individualCountResult =
            Interpreter.interpretIndividualCount(values, headersByName);
        setResult(descriptorDto, individualCountResult, DescriptorDto::setIndividualCount);

        // identifiedBy
        InterpretedResult<List<String>> identifiedByResult =
            Interpreter.interpretStringList(values, headersByName, DwcTerm.identifiedBy);
        setResult(descriptorDto, identifiedByResult, DescriptorDto::setIdentifiedBy);

        // dateIdentified
        InterpretedResult<Date> dateIdentifiedResult =
            Interpreter.interpretDateIdentified(values, headersByName);
        setResult(descriptorDto, dateIdentifiedResult, DescriptorDto::setDateIdentified);

        // TypeStatus
        InterpretedResult<List<String>> typeStatusResult =
            Interpreter.interpretTypeStatus(values, headersByName);
        setResult(descriptorDto, typeStatusResult, DescriptorDto::setTypeStatus);

        // recordedBy
        InterpretedResult<List<String>> recordedByResult =
            Interpreter.interpretStringList(values, headersByName, DwcTerm.recordedBy);
        setResult(descriptorDto, recordedByResult, DescriptorDto::setRecordedBy);

        // TODO: create ltc terms??
        // discipline
        InterpretedResult<String> disciplineResult =
            Interpreter.interpretString(values, headersByName, "ltc:discipline");
        setResult(descriptorDto, disciplineResult, DescriptorDto::setDiscipline);

        // objectClassification
        InterpretedResult<String> objectClassificationResult =
            Interpreter.interpretString(values, headersByName, "ltc:objectClassificationName");
        setResult(
            descriptorDto, objectClassificationResult, DescriptorDto::setObjectClassificationName);

        descriptorsMapper.createDescriptor(descriptorDto);

        // verbatim fields
        for (int i = 0; i < values.length; i++) {
          descriptorsMapper.createVerbatim(
              descriptorDto.getKey(), headersByIndex.get(i), values[i]);
        }
      }
    }
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

  @Override
  public void deleteDescriptorGroup(@NotNull long key) {
    DescriptorGroup descriptorGroup = descriptorsMapper.getDescriptorGroup(key);
    Preconditions.checkArgument(
        descriptorGroup != null, "Descriptor group not found for key " + key);
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
  @Override
  public void updateDescriptorGroup(
      @NotNull long descriptorGroupKey,
      @NotNull byte[] descriptorGroupFile,
      @NotNull ExportFormat format,
      @NotNull String title,
      String description) {
    Objects.requireNonNull(descriptorGroupFile);
    Preconditions.checkArgument(descriptorGroupFile.length > 0);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(title));

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();

    DescriptorGroup descriptorGroup = descriptorsMapper.getDescriptorGroup(descriptorGroupKey);
    descriptorGroup.setTitle(title);
    descriptorGroup.setDescription(description);
    descriptorGroup.setModifiedBy(username);
    descriptorsMapper.updateDescriptorGroup(descriptorGroup);

    // remove descriptors
    descriptorsMapper.deleteDescriptors(descriptorGroup.getKey());

    // reimport the file
    importDescriptorsFile(descriptorGroupFile, format, descriptorGroup.getKey());

    eventManager.post(
      SubEntityCollectionEvent.newInstance(
        descriptorGroup.getCollectionKey(),
        Collection.class,
        DescriptorGroup.class,
        descriptorGroupKey,
        EventType.UPDATE));
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
        searchRequest.getQuery() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQuery()))
            : searchRequest.getQuery();

    DescriptorGroupParams params =
        DescriptorGroupParams.builder()
            .query(query)
            .collectionKey(collectionKey)
            .title(searchRequest.getTitle())
            .description(searchRequest.getDescription())
            .deleted(searchRequest.getDeleted())
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
        searchRequest.getQuery() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQuery()))
            : searchRequest.getQuery();

    return DescriptorParams.builder()
        .query(query)
        .descriptorGroupKey(searchRequest.getDescriptorGroupKey())
        .country(searchRequest.getCountry())
        .dateIdentifiedBefore(searchRequest.getDateIdentifiedBefore())
        .dateIdentifiedFrom(searchRequest.getDateIdentifiedFrom())
        .discipline(searchRequest.getDiscipline())
        .individualCount(parseIntegerRangeParameter(searchRequest.getIndividualCount()))
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
    List<VerbatimDto> dtos = descriptorsMapper.getVerbatimNames(descriptorGroupKey);
    return dtos.stream()
        .sorted(Comparator.comparing(VerbatimDto::getKey))
        .map(VerbatimDto::getFieldName)
        .collect(Collectors.toCollection(LinkedHashSet::new));
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

    Map<String, String> verbatim = new LinkedHashMap<>();
    dto.getVerbatim().stream()
        .sorted(Comparator.comparing(VerbatimDto::getKey))
        .forEach(v -> verbatim.put(v.getFieldName(), v.getFieldValue()));
    descriptorRecord.setVerbatim(verbatim);
    return descriptorRecord;
  }
}
