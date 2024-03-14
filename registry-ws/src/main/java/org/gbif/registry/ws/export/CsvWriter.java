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
package org.gbif.registry.ws.export;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.*;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.registry.*;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.supercsv.cellprocessor.*;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Builder
public class CsvWriter<T> {

  // Delimiter used for list/array of elements
  public static final String ARRAY_DELIMITER = ";";

  private final String[] header;

  private final String[] fields;

  private final CellProcessor[] processors;

  private final Iterable<T> pager;

  private final ExportFormat preference;

  // Use dozer if set to true.
  private Class<?> forClass;

  private CsvPreference csvPreference() {
    if (ExportFormat.CSV == preference) {
      return CsvPreference.STANDARD_PREFERENCE;
    } else if (ExportFormat.TSV == preference) {
      return CsvPreference.TAB_PREFERENCE;
    }
    throw new IllegalArgumentException("Export format not supported " + preference);
  }

  @SneakyThrows
  public void export(Writer writer) {
    if (forClass != null) {
      exportUsingDozerBeanWriter(writer);
    } else {
      exportUsingBeanWriter(writer);
    }
  }

  @SneakyThrows
  private void exportUsingBeanWriter(Writer writer) {
    try (ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, csvPreference())) {
      beanWriter.writeHeader(header);
      for (T o : pager) {
        beanWriter.write(o, fields, processors);
      }
    }
  }

  @SneakyThrows
  private void exportUsingDozerBeanWriter(Writer writer) {
    try (CsvDozerBeanWriter beanWriter = new CsvDozerBeanWriter(writer, csvPreference())) {
      beanWriter.writeHeader(header);
      beanWriter.configureBeanMapping(forClass, fields);
      for (T o : pager) {
        beanWriter.write(o, processors);
      }
    }
  }

  /** Creates an CsvWriter/exporter of DownloadStatistics. */
  public static CsvWriter<DownloadStatistics> downloadStatisticsCsvWriter(
      Iterable<DownloadStatistics> pager, ExportFormat preference) {

    return CsvWriter.<DownloadStatistics>builder()
        .fields(new String[] {"datasetKey", "totalRecords", "numberDownloads", "year", "month"})
        .header(new String[] {"dataset_key", "total_records", "number_downloads", "year", "month"})
        .processors(
            new CellProcessor[] {
              new UUIDProcessor(), // datasetKey
              new Optional(new ParseLong()), // totalRecords
              new Optional(new ParseLong()), // numberDowloads
              new Optional(new ParseInt()), // year
              new Optional(new ParseInt()) // month
            })
        .preference(preference)
        .pager(pager)
        .build();
  }

  /** Creates an CsvWriter/exporter of DatasetSearchResult. */
  public static CsvWriter<DatasetSearchResult> datasetSearchResultCsvWriter(
      Iterable<DatasetSearchResult> pager, ExportFormat preference) {
    return CsvWriter.<DatasetSearchResult>builder()
        .fields(
            new String[] {
              "key",
              "title",
              "doi",
              "license",
              "type",
              "subType",
              "hostingOrganizationKey",
              "hostingOrganizationTitle",
              "hostingCountry",
              "publishingOrganizationKey",
              "publishingOrganizationTitle",
              "publishingCountry",
              "endorsingNodeKey",
              "networkKeys",
              "projectIdentifier",
              "recordCount",
              "nameUsagesCount"
            })
        .header(
            new String[] {
              "dataset_key",
              "title",
              "doi",
              "license",
              "type",
              "sub_type",
              "hosting_organization_key",
              "hosting_organization_title",
              "hosting_country",
              "publishing_organization_key",
              "publishing_organization_title",
              "publishing_country",
              "endorsing_node_key",
              "network_keys",
              "project_identifier",
              "occurrence_records_count",
              "name_usages_count"
            })
        //  "recordCount", "nameUsagesCount"
        .processors(
            new CellProcessor[] {
              new UUIDProcessor(), // key
              new CleanStringProcessor(), // title
              new DOIProcessor(), // doi
              new Optional(new ParseEnum(License.class)), // license
              new Optional(new ParseEnum(DatasetType.class)), // type
              new Optional(new ParseEnum(DatasetSubtype.class)), // subType
              new UUIDProcessor(), // hostingOrganizationKey
              new CleanStringProcessor(), // hostingOrganizationTitle
              new CountryProcessor(), // hostingCountry
              new UUIDProcessor(), // publishingOrganizationKey
              new CleanStringProcessor(), // publishingOrganizationTitle
              new CountryProcessor(), // publishingCountry
              new UUIDProcessor(), // endorsingNodeKey
              new ListUUIDProcessor(), // networkKeys
              new CleanStringProcessor(), // projectIdentifier
              new Optional(new ParseInt()), // recordCount
              new Optional(new ParseInt()) // nameUsagesCount
            })
        .preference(preference)
        .pager(pager)
        .build();
  }

  /** Creates an CsvWriter/exporter of DatasetOccurrenceDownloadUsage. */
  public static CsvWriter<DatasetOccurrenceDownloadUsage> datasetOccurrenceDownloadUsageCsvWriter(
      Iterable<DatasetOccurrenceDownloadUsage> pager, ExportFormat preference) {
    return CsvWriter.<DatasetOccurrenceDownloadUsage>builder()
        .fields(
            new String[] {
              "datasetDOI", "datasetKey", "datasetTitle", "datasetCitation", "numberRecords"
            })
        .header(
            new String[] {
              "dataset_doi", "dataset_key", "dataset_title", "dataset_citation", "number_records"
            })
        .processors(
            new CellProcessor[] {
              new DOIProcessor(), // datasetDOI
              new UUIDProcessor(), // datasetKey
              new CleanStringProcessor(), // datasetTitle
              new CleanStringProcessor(), // datasetCitation
              new Optional(new ParseLong()) // numberRecords
            })
        .preference(preference)
        .pager(pager)
        .build();
  }

  /** Creates an CsvWriter/exporter of Collection. */
  public static CsvWriter<CollectionView> collections(
      Iterable<CollectionView> pager, ExportFormat preference) {
    return CsvWriter.<CollectionView>builder()
        .fields(
            new String[] {
              "collection.key",
              "collection.code",
              "collection.name",
              "collection.description",
              "collection.country",
              "collection.city",
              "collection.province",
              "collection.contentTypes",
              "collection.active",
              "collection.personalCollection",
              "collection.doi",
              "collection.email",
              "collection.phone",
              "collection.homepage",
              "collection.catalogUrls",
              "collection.apiUrls",
              "collection.preservationTypes",
              "collection.accessionStatus",
              "institutionName",
              "institutionCode",
              "collection.institutionKey",
              "collection.mailingAddress",
              "collection.address",
              "collection.createdBy",
              "collection.modifiedBy",
              "collection.created",
              "collection.modified",
              "collection.deleted",
              "collection.tags",
              "collection.identifiers",
              "collection.contactPersons",
              "collection.numberSpecimens",
              "collection.machineTags",
              "collection.taxonomicCoverage",
              "collection.geographicCoverage",
              "collection.notes",
              "collection.incorporatedCollections",
              "collection.importantCollectors",
              "collection.collectionSummary",
              "collection.alternativeCodes",
              "collection.comments",
              "collection.occurrenceMappings",
              "collection.replacedBy"
            })
        .header(
            new String[] {
              "key",
              "code",
              "name",
              "description",
              "country",
              "city",
              "province",
              "content_types",
              "active",
              "personal_collection",
              "doi",
              "email",
              "phone",
              "homepage",
              "catalog_url",
              "api_url",
              "preservation_types",
              "accession_status",
              "institution_name",
              "institution_code",
              "institution_key",
              "mailing_address",
              "address",
              "created_by",
              "modified_by",
              "created",
              "modified",
              "deleted",
              "tags",
              "identifiers",
              "contactPersons",
              "number_specimens",
              "machine_tags",
              "taxonomic_coverage",
              "geographic_coverage",
              "notes",
              "incorporated_collections",
              "important_collectors",
              "collection_summary",
              "alternative_codes",
              "comments",
              "occurrence_mappings",
              "replaced_by"
            })
        .processors(
            new CellProcessor[] {
              new UUIDProcessor(), // key: UUID
              null, // code: String
              new CleanStringProcessor(), // name: String
              new CleanStringProcessor(), // description: String
              new CleanStringProcessor(), // address: extract the country
              new CleanStringProcessor(), // address: extract the city
              new CleanStringProcessor(), // address: extract the province
              new ListStringProcessor(), // contentTypes: List
              new Optional(new FmtBool("true", "false")), // active: boolean
              new Optional(new FmtBool("true", "false")), // personalCollection: boolean
              new DOIProcessor(), // doi: DOI
              new ListStringProcessor(), // email: List
              new ListStringProcessor(), // phone: List
              new UriProcessor(), // homepage: URI
              new ListUriProcessor(), // catalogUrl: URI
              new ListUriProcessor(), // apiUrl: URI
              new ListPreservationTypeProcessor(), // preservationTypes: List
              new Optional(
                  new ParseEnum(AccessionStatus.class)), // accessionStatus: AccessionStatus
              null, // institutionCode: String
              new CleanStringProcessor(), // institutionName: String
              new UUIDProcessor(), // institutionKey: UUID
              new AddressProcessor(), // mailingAddress: Address
              new AddressProcessor(), // address: Address
              null, // createdBy: String
              null, // modifiedBy: String
              new FmtDate(StdDateFormat.DATE_FORMAT_STR_ISO8601), // created: Date
              new FmtDate(StdDateFormat.DATE_FORMAT_STR_ISO8601), // modified: Date
              new Optional(new FmtDate(StdDateFormat.DATE_FORMAT_STR_ISO8601)), // deleted: Date
              new ListTagsProcessor(), // tags: List
              new ListIdentifierProcessor(), // identifiers: List
              new ListContactProcessor(), // contactPersons: List
              new Optional(new ParseInt()), // numberSpecimens: int
              new ListMachineTagProcessor(), // machineTags: List
              new CleanStringProcessor(), // taxonomicCoverage: String
              new CleanStringProcessor(), // geographicCoverage: String
              new CleanStringProcessor(), // notes: String
              new ListStringProcessor(), // incorporatedCollections: List
              new ListStringProcessor(), // importantCollectors: List
              new CollectionSummaryProcessor(), // collectionSummary: Map
              new ListAlternativeCodeProcessor(), // alternativeCodes: List
              new ListCommentProcessor(), // comments: List
              new ListOccurrenceMappingsProcessor(), // occurrenceMappings: List
              new UUIDProcessor() // replacedBy: UUID
            })
        .forClass(CollectionView.class)
        .preference(preference)
        .pager(pager)
        .build();
  }

  /** Creates an CsvWriter/exporter of Collection. */
  // TODO: processor for new multivalue fields
  public static CsvWriter<Institution> institutions(
      Iterable<Institution> pager, ExportFormat preference) {
    return CsvWriter.<Institution>builder()
        .fields(
            new String[] {
              "key",
              "code",
              "name",
              "description",
              "country",
              "city",
              "province",
              "types",
              "active",
              "email",
              "phone",
              "homepage",
              "catalogUrls",
              "apiUrls",
              "institutionalGovernances",
              "disciplines",
              "latitude",
              "longitude",
              "mailingAddress",
              "address",
              "additionalNames",
              "foundingDate",
              "numberSpecimens",
              "logoUrl",
              "createdBy",
              "modifiedBy",
              "created",
              "modified",
              "deleted",
              "tags",
              "identifiers",
              "contactPersons",
              "machineTags",
              "alternativeCodes",
              "comments",
              "occurrenceMappings",
              "replacedBy",
              "convertedToCollection"
            })
        .header(
            new String[] {
              "key",
              "code",
              "name",
              "description",
              "country",
              "city",
              "province",
              "type",
              "active",
              "email",
              "phone",
              "homepage",
              "catalog_url",
              "api_url",
              "institutional_governance",
              "disciplines",
              "latitude",
              "longitude",
              "mailing_address",
              "address",
              "additional_names",
              "founding_date",
              "number_specimens",
              "logo_url",
              "created_by",
              "modified_by",
              "created",
              "modified",
              "deleted",
              "tags",
              "identifiers",
              "contactPersons",
              "machine_tags",
              "alternative_codes",
              "comments",
              "occurrence_mappings",
              "replaced_by",
              "converted_to_collection"
            })
        .processors(
            new CellProcessor[] {
              new UUIDProcessor(), // key: UUID
              null, // code: String
              new CleanStringProcessor(), // name: String
              new CleanStringProcessor(), // description: String
              new CleanStringProcessor(), // address: extract the country
              new CleanStringProcessor(), // address: extract the city
              new CleanStringProcessor(), // address: extract the province
              new Optional(new ListInstitutionTypeProcessor()), // type:InstitutionType
              new Optional(new FmtBool("true", "false")), // active: boolean
              new ListStringProcessor(), // email: List<String>
              new ListStringProcessor(), // phone: List<String>
              new UriProcessor(), // homepage: URI
              new ListUriProcessor(), // catalogUrl: URI
              new ListUriProcessor(), // apiUrl: URI
              new Optional(
                  new ListInstitutionGovernanceProcessor()), // institutionalGovernance:InstitutionGovernance
              new ListStringProcessor(), // disciplines:List
              new Optional(new FmtNumber("###.####")), // latitude: BigDecimal
              new Optional(new FmtNumber("###.####")), // longitude: BigDecimal
              new AddressProcessor(), // mailingAddress: Address
              new AddressProcessor(), // address: Address
              new ListStringProcessor(), // additionalNames: List<String>
              new Optional(new ParseInt()), // foundingDate: Date
              new Optional(new ParseInt()), // numberSpecimens: int
              new UriProcessor(), // logoUrl: URI
              null, // createdBy: String
              null, // modifiedBy: String
              new FmtDate(StdDateFormat.DATE_FORMAT_STR_ISO8601), // created: Date
              new FmtDate(StdDateFormat.DATE_FORMAT_STR_ISO8601), // modified: Date
              new Optional(new FmtDate(StdDateFormat.DATE_FORMAT_STR_ISO8601)), // deleted: Date
              new ListTagsProcessor(), // tags: List<Tag>
              new ListIdentifierProcessor(), // identifiers: List<Identifier>
              new ListContactProcessor(), // contactPersons: List<Contact>
              new ListMachineTagProcessor(), // machineTags: List<MachineTag>
              new ListAlternativeCodeProcessor(), // alternativeCodes: List<AlternativeCoe>
              new ListCommentProcessor(), // comments: List<Comment>
              new ListOccurrenceMappingsProcessor(), // occurrenceMappings: List<OccurrenceMapping>
              new UUIDProcessor(), // replacedBy: UUID
              new UUIDProcessor() // convertedToCollection: UUID
            })
        .preference(preference)
        .pager(pager)
        .build();
  }

  /** Null aware UUID processor. */
  public static class UUIDProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? ((UUID) value).toString() : "";
    }
  }

  /** Null aware List of UUIDs processor. */
  public static class ListUUIDProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null
          ? ((List<UUID>) value)
              .stream().map(UUID::toString).collect(Collectors.joining(ARRAY_DELIMITER))
          : "";
    }
  }

  /** Null aware UUID processor. */
  public static class DOIProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? value.toString() : "";
    }
  }

  /** Null aware Country processor. */
  public static class CountryProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? ((Country) value).getIso2LetterCode() : "";
    }
  }

  /**
   * Produces a String instance clean of delimiter. If the value is null an empty string is
   * returned. Borrowed from Occurrence Downloads!!.
   */
  public static class CleanStringProcessor implements CellProcessor {

    private static final String DELIMETERS_MATCH =
        "\\t|\\n|\\r|(?:(?>\\u000D\\u000A)|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029\\u0000])";

    private static final Pattern DELIMETERS_MATCH_PATTERN = Pattern.compile(DELIMETERS_MATCH);

    public static String cleanString(String value) {
      return DELIMETERS_MATCH_PATTERN.matcher(value).replaceAll(" ");
    }

    @Override
    public String execute(Object value, CsvContext context) {
      return value != null ? CleanStringProcessor.cleanString((String) value) : "";
    }
  }

  /** Null aware List of PreservationType processor. */
  public static class ListPreservationTypeProcessor implements CellProcessor {

    public static String toString(List<PreservationType> value) {
      return value.stream()
          .map(PreservationType::name)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<PreservationType>) value) : "";
    }
  }

  /** Null aware List<String> processor. */
  public static class ListStringProcessor implements CellProcessor {

    public static String toString(List<String> value) {
      return value.stream()
          .map(CleanStringProcessor::cleanString)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<String>) value) : "";
    }
  }

  /** Null aware List<URI> processor. */
  public static class ListUriProcessor implements CellProcessor {

    public static String toString(List<URI> value) {
      return value.stream().map(URI::toString).collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<URI>) value) : "";
    }
  }

  /** Null aware Uri processor. */
  public static class UriProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? value.toString() : "";
    }
  }

  /** Joins elements using as a delimiter. */
  public static String notNullJoiner(String delimiter, String... elements) {
    return Arrays.stream(elements)
        .filter(s -> s != null && !s.isEmpty())
        .collect(Collectors.joining(delimiter));
  }

  /** Null aware Uri processor. */
  public static class AddressProcessor implements CellProcessor {

    public static String toString(Address address) {
      return CleanStringProcessor.cleanString(
          notNullJoiner(
              " ",
              address.getAddress(),
              address.getCity(),
              address.getProvince(),
              address.getPostalCode(),
              address.getCountry() != null ? address.getCountry().getTitle() : ""));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString(((Address) value)) : "";
    }
  }

  /** Null aware List<Tags> processor. */
  public static class ListTagsProcessor implements CellProcessor {

    public static String toString(List<Tag> value) {
      return value.stream()
          .map(t -> CleanStringProcessor.cleanString(t.getValue()))
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<Tag>) value) : "";
    }
  }

  /** Null aware List<Identifier> processor. */
  public static class ListIdentifierProcessor implements CellProcessor {

    public static String toString(List<Identifier> value) {
      return value.stream()
          .map(Identifier::getIdentifier)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<Identifier>) value) : "";
    }
  }

  /** Null aware List<MachineTag> processor. */
  public static class ListMachineTagProcessor implements CellProcessor {

    public static String toString(List<MachineTag> value) {
      return value.stream()
          .map(ListMachineTagProcessor::toString)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    public static String toString(MachineTag machineTag) {
      return notNullJoiner(
          ":", machineTag.getNamespace(), machineTag.getName(), machineTag.getValue());
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<MachineTag>) value) : "";
    }
  }

  /** Null aware List<Person> processor. */
  public static class ListContactProcessor implements CellProcessor {

    public static String toString(List<Contact> value) {
      return value.stream()
          .map(ListContactProcessor::toString)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    public static String toString(Contact contact) {
      return CleanStringProcessor.cleanString(
          notNullJoiner(
              " ",
              contact.getFirstName(),
              contact.getLastName(),
              notNullJoiner(" ", contact.getPhone().toArray(new String[0])),
              notNullJoiner(" ", contact.getEmail().toArray(new String[0])),
              notNullJoiner(" ", contact.getPosition().toArray(new String[0])),
              notNullJoiner(" ", contact.getTaxonomicExpertise().toArray(new String[0]))));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<Contact>) value) : "";
    }
  }

  /** Null aware List<AlternativeCode> processor. */
  public static class ListAlternativeCodeProcessor implements CellProcessor {

    public static String toString(List<AlternativeCode> value) {
      return value.stream()
          .map(AlternativeCode::getCode)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<AlternativeCode>) value) : "";
    }
  }

  /** Null aware List<Comment> processor. */
  public static class ListCommentProcessor implements CellProcessor {

    public static String toString(List<Comment> value) {
      return value.stream()
          .map(c -> CleanStringProcessor.cleanString(c.getContent()))
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<Comment>) value) : "";
    }
  }

  /** Null aware Map<String, Integer> processor. */
  public static class CollectionSummaryProcessor implements CellProcessor {

    public static String toString(Map<String, Integer> value) {
      return value.entrySet().stream()
          .map(e -> e.getKey() + ':' + e.getValue().toString())
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((Map<String, Integer>) value) : "";
    }
  }

  /** Null aware List<OccurrenceMapping> processor. */
  public static class ListOccurrenceMappingsProcessor implements CellProcessor {

    public static String toString(List<OccurrenceMapping> value) {
      return value.stream()
          .map(ListOccurrenceMappingsProcessor::toString)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    public static String toString(OccurrenceMapping occurrenceMapping) {
      return notNullJoiner(
          ":",
          occurrenceMapping.getCode(),
          occurrenceMapping.getIdentifier(),
          occurrenceMapping.getDatasetKey().toString());
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<OccurrenceMapping>) value) : "";
    }
  }

  /** Null aware List<Discipline> processor. */
  public static class ListInstitutionTypeProcessor implements CellProcessor {

    public static String toString(List<InstitutionType> value) {
      return value.stream().map(InstitutionType::name).collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<InstitutionType>) value) : "";
    }
  }

  /** Null aware List<Discipline> processor. */
  public static class ListInstitutionGovernanceProcessor implements CellProcessor {

    public static String toString(List<InstitutionGovernance> value) {
      return value.stream()
          .map(InstitutionGovernance::name)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? toString((List<InstitutionGovernance>) value) : "";
    }
  }
}
