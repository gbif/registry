package org.gbif.registry.service.collections.descriptors;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.Record;
import org.gbif.api.model.collections.descriptors.VerbatimField;
import org.gbif.api.model.collections.request.DescriptorRecordsSearchRequest;
import org.gbif.api.model.collections.request.DescriptorsSearchRequest;
import org.gbif.api.model.collections.request.DescriptorsVerbatimFieldsSearchRequest;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.checklistbank.ws.client.NubResourceClient;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.registry.persistence.mapper.collections.DescriptorsMapper;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorRecordsParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsVerbatimFieldsParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiConsumer;

import static org.gbif.registry.service.collections.utils.ParamUtils.parseIntegerRangeParameter;

@Validated
@Service
public class DefaultDescriptorService implements DescriptorsService {

  private final NubResourceClient nubResourceClient;
  private final DescriptorsMapper descriptorsMapper;

  @Autowired
  public DefaultDescriptorService(
      NubResourceClient nubResourceClient, DescriptorsMapper descriptorsMapper) {
    this.nubResourceClient = nubResourceClient;
    this.descriptorsMapper = descriptorsMapper;
  }

  @SneakyThrows
  @Override
  public long createDescriptor(
      @NotNull @Valid byte[] descriptorFile,
      @NotNull ExportFormat format,
      @NotNull String title,
      String description,
      @NotNull UUID collectionKey) {
    Objects.requireNonNull(descriptorFile);
    Preconditions.checkArgument(descriptorFile.length > 0);
    Objects.requireNonNull(collectionKey);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(title));

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();

    Descriptor descriptor = new Descriptor();
    descriptor.setTitle(title);
    descriptor.setDescription(description);
    descriptor.setCreatedBy(username);
    descriptor.setModifiedBy(username);
    descriptor.setCollectionKey(collectionKey);
    descriptorsMapper.createDescriptor(descriptor);

    importDescriptorsFile(descriptorFile, format, descriptor.getKey());

    return descriptor.getKey();
  }

  private void importDescriptorsFile(
      @NotNull @Valid byte[] descriptorFile, ExportFormat format, long descriptorKey)
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
        // TODO: lowecase headers and compare case insensitive
        headersByIndex.put(i, headers[i]);
        headersByName.put(headers[i], i);
      }

      // TODO: fill CSV empty columns

      String[] values;
      while ((values = csvReader.readNextSilently()) != null) {
        if (values.length == 0) {
          continue;
        }

        Record descriptorRecord = new Record();
        descriptorRecord.setDescriptorKey(descriptorKey);

        // sciName
        InterpretedResult<String> taxonomyResult =
            Interpreter.interpretScientificName(values, headersByName, nubResourceClient);
        setResult(descriptorRecord, taxonomyResult, Record::setScientificName);

        // country
        InterpretedResult<Country> countryResult =
            Interpreter.interpretCountry(values, headersByName);
        setResult(descriptorRecord, countryResult, Record::setCountry);

        // individual count
        InterpretedResult<Integer> individualCountResult =
            Interpreter.interpretIndividualCount(values, headersByName);
        setResult(descriptorRecord, individualCountResult, Record::setIndividualCount);

        // identifiedBy
        InterpretedResult<List<String>> identifiedByResult =
            Interpreter.interpretStringList(values, headersByName, DwcTerm.identifiedBy);
        setResult(descriptorRecord, identifiedByResult, Record::setIdentifiedBy);

        // dateIdentified
        InterpretedResult<Date> dateIdentifiedResult =
            Interpreter.interpretDateIdentified(values, headersByName);
        setResult(descriptorRecord, dateIdentifiedResult, Record::setDateIdentified);

        // TypeStatus
        InterpretedResult<List<TypeStatus>> typeStatusResult =
            Interpreter.interpretTypeStatus(values, headersByName);
        setResult(descriptorRecord, typeStatusResult, Record::setTypeStatus);

        // recordedBy
        InterpretedResult<List<String>> recordedByResult =
            Interpreter.interpretStringList(values, headersByName, DwcTerm.recordedBy);
        setResult(descriptorRecord, recordedByResult, Record::setRecordedBy);

        // TODO: create ltc terms??
        // discipline
        InterpretedResult<String> disciplineResult =
            Interpreter.interpretString(values, headersByName, "ltc:discipline");
        setResult(descriptorRecord, disciplineResult, Record::setDiscipline);

        // objectClassification
        InterpretedResult<String> objectClassificationResult =
            Interpreter.interpretString(values, headersByName, "ltc:objectClassificationName");
        setResult(descriptorRecord, objectClassificationResult, Record::setObjectClassification);

        descriptorsMapper.createRecord(descriptorRecord);

        // verbatim fields
        for (int i = 0; i < values.length; i++) {
          VerbatimField verbatimField = new VerbatimField();
          verbatimField.setRecordKey(descriptorRecord.getKey());
          verbatimField.setFieldName(headersByIndex.get(i));
          verbatimField.setFieldValue(values[i]);
          descriptorsMapper.createVerbatim(verbatimField);
        }
      }
    }
  }

  private <T> void setResult(
      Record descriptorRecord, InterpretedResult<T> result, BiConsumer<Record, T> setter) {
    setter.accept(descriptorRecord, result.getResult());
    if (descriptorRecord.getIssues() == null) {
      descriptorRecord.setIssues(new ArrayList<>());
    }
    if (result.getIssues() != null) {
      descriptorRecord.getIssues().addAll(result.getIssues());
    }
  }

  @Override
  public void deleteDescriptor(@NotNull long key) {
    descriptorsMapper.deleteDescriptor(key);
  }

  @Override
  public Descriptor getDescriptor(@NotNull long key) {
    return descriptorsMapper.getDescriptor(key);
  }

  @SneakyThrows
  @Override
  public void updateDescriptor(
      @NotNull long descriptorKey,
      @NotNull byte[] descriptorsFile,
      @NotNull ExportFormat format,
      @NotNull String title,
      String description) {
    Objects.requireNonNull(descriptorsFile);
    Preconditions.checkArgument(descriptorsFile.length > 0);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(title));

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();

    Descriptor descriptor = descriptorsMapper.getDescriptor(descriptorKey);
    descriptor.setTitle(title);
    descriptor.setDescription(description);
    descriptor.setModifiedBy(username);
    descriptorsMapper.updateDescriptor(descriptor);

    // remove records
    descriptorsMapper.deleteRecords(descriptor.getKey());

    // reimport the file
    importDescriptorsFile(descriptorsFile, format, descriptor.getKey());
  }

  @Override
  public PagingResponse<Descriptor> listDescriptors(
      @NotNull UUID collectionKey, DescriptorsSearchRequest searchRequest) {
    Objects.requireNonNull(collectionKey);
    if (searchRequest == null) {
      searchRequest = DescriptorsSearchRequest.builder().build();
    }

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();
    String query =
        searchRequest.getQuery() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQuery()))
            : searchRequest.getQuery();

    DescriptorsParams params =
        DescriptorsParams.builder()
            .query(query)
            .collectionKey(collectionKey)
            .title(searchRequest.getTitle())
            .description(searchRequest.getDescription())
            .page(page)
            .build();

    return new PagingResponse<>(
        page,
        descriptorsMapper.countDescriptors(params),
        descriptorsMapper.listDescriptors(params));
  }

  @Override
  public Record getDescriptorRecord(@NotNull long key) {
    return descriptorsMapper.getRecord(key);
  }

  @Override
  public PagingResponse<Record> listDescriptorRecords(
      DescriptorRecordsSearchRequest searchRequest) {
    if (searchRequest == null) {
      searchRequest = DescriptorRecordsSearchRequest.builder().build();
    }

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();
    String query =
        searchRequest.getQuery() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQuery()))
            : searchRequest.getQuery();

    DescriptorRecordsParams params =
        DescriptorRecordsParams.builder()
            .query(query)
            .descriptorKey(searchRequest.getDescriptorKey())
            .country(searchRequest.getCountry())
            .dateIdentified(searchRequest.getDateIdentified())
            .dateIdentifiedBefore(searchRequest.getDateIdentifiedBefore())
            .dateIdentifiedFrom(searchRequest.getDateIdentifiedFrom())
            .discipline(searchRequest.getDiscipline())
            .individualCount(parseIntegerRangeParameter(searchRequest.getIndividualCount()))
            .scientificName(searchRequest.getScientificName())
            .objectClassification(searchRequest.getObjectClassification())
            .recordedBy(searchRequest.getRecordedBy())
            .identifiedBy(searchRequest.getIdentifiedBy())
            .issues(searchRequest.getIssues())
            .typeStatus(searchRequest.getTypeStatus())
            .page(page)
            .build();

    return new PagingResponse<>(
        page, descriptorsMapper.countRecords(params), descriptorsMapper.listRecords(params));
  }

  @Override
  public PagingResponse<VerbatimField> listDescriptorRecordVerbatimFields(
      DescriptorsVerbatimFieldsSearchRequest searchRequest) {
    if (searchRequest == null) {
      searchRequest = DescriptorsVerbatimFieldsSearchRequest.builder().build();
    }

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();
    String query =
        searchRequest.getQuery() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQuery()))
            : searchRequest.getQuery();

    DescriptorsVerbatimFieldsParams params =
        DescriptorsVerbatimFieldsParams.builder()
            .query(query)
            .recordKey(searchRequest.getRecordKey())
            .page(page)
            .build();

    return new PagingResponse<>(
        page, descriptorsMapper.countVerbatims(params), descriptorsMapper.listVerbatims(params));
  }
}
