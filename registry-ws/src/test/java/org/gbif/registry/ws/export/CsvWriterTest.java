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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.CollectionContentType;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.api.vocabulary.collections.PreservationType;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvWriterTest {

  /**
   * Functional interface to test a single line.
   */
  private interface AssertElement<T> {
    void assertElement(T element, String[] line);
  }

  /**
   * Tests a report against a source list fo elements.
   */
  private <T> void assertExport(
      List<T> source, StringWriter writer, CsvWriter csvWriter, AssertElement<T> assertElement) {
    String export = writer.toString();
    String[] lines = export.split("\\n");

    // Number of lines is header + list.size
    assertEquals(source.size() + 1, lines.length);

    // Each line has csvWriter.getHeader().length - 1 commands
    assertEquals(
        (source.size() + 1) * (csvWriter.getFields().length - 1),
        export.chars().filter(ch -> ch == csvWriter.getPreference().getDelimiter()).count());
    IntStream.range(0, source.size())
        .forEach(
            idx ->
                assertElement.assertElement(
                    source.get(idx),
                    lines[idx + 1].split(csvWriter.getPreference().getDelimiter().toString())));
  }

  /**
   * Test one DownloadStatistic against its expected exported data.
   */
  private void assertDownloadStatistics(DownloadStatistics downloadStatistics, String line[]) {
    assertEquals(downloadStatistics.getDatasetKey().toString(), line[0]);
    assertEquals(downloadStatistics.getTotalRecords(), Integer.parseInt(line[1]));
    assertEquals(downloadStatistics.getNumberDownloads(), Integer.parseInt(line[2]));
    assertEquals(downloadStatistics.getYear(), Integer.parseInt(line[3]));
    assertEquals(downloadStatistics.getMonth(), Integer.parseInt(line[4]));
  }

  @Test
  public void downloadStatisticsTest() {

    // Test data
    List<DownloadStatistics> stats =
        Arrays.asList(
            new DownloadStatistics(UUID.randomUUID(), 10L, 10L, LocalDate.of(2020, 1, 1)),
            new DownloadStatistics(UUID.randomUUID(), 10L, 10L, LocalDate.of(2021, 2, 1)));

    StringWriter writer = new StringWriter();

    CsvWriter<DownloadStatistics> csvWriter =
        CsvWriter.downloadStatisticsCsvWriter(stats, ExportFormat.TSV);
    csvWriter.export(writer);

    // Assert elements
    assertExport(stats, writer, csvWriter, this::assertDownloadStatistics);
  }

  /**
   * Generates a DatasetSearchResult, the consecutive parameters is used as postfix for titles,
   * projectIdentifier, occurrence and name usages counts.
   */
  private DatasetSearchResult newDatasetSearchResult(int consecutive) {
    DatasetSearchResult datasetSearchResult = new DatasetSearchResult();
    datasetSearchResult.setKey(UUID.randomUUID());
    datasetSearchResult.setTitle("DatasetTitle" + consecutive);
    datasetSearchResult.setDoi(new DOI("10.21373/6m9yw" + consecutive));
    datasetSearchResult.setLicense(License.CC_BY_4_0);
    datasetSearchResult.setType(DatasetType.OCCURRENCE);
    datasetSearchResult.setSubtype(DatasetSubtype.DERIVED_FROM_OCCURRENCE);
    datasetSearchResult.setHostingOrganizationKey(UUID.randomUUID());
    datasetSearchResult.setHostingOrganizationTitle("HostingOrganizationTitle" + consecutive);
    datasetSearchResult.setHostingCountry(Country.DENMARK);
    datasetSearchResult.setPublishingOrganizationKey(UUID.randomUUID());
    datasetSearchResult.setPublishingOrganizationTitle("PublishingOrganizationTitle" + consecutive);
    datasetSearchResult.setPublishingCountry(Country.COSTA_RICA);
    datasetSearchResult.setEndorsingNodeKey(UUID.randomUUID());
    datasetSearchResult.setNetworkKeys(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));
    datasetSearchResult.setProjectIdentifier("project" + consecutive);
    datasetSearchResult.setRecordCount(consecutive);
    datasetSearchResult.setNameUsagesCount(consecutive);
    return datasetSearchResult;
  }

  /**
   * Test one DatasetSearchResult against its expected exported data.
   */
  private void assertDatasetSearchResult(DatasetSearchResult datasetSearchResult, String[] line) {
    assertEquals(datasetSearchResult.getKey().toString(), line[0]);
    assertEquals(datasetSearchResult.getTitle(), line[1]);
    assertEquals(datasetSearchResult.getDoi().toString(), line[2]);
    assertEquals(datasetSearchResult.getLicense().name(), line[3]);
    assertEquals(datasetSearchResult.getType().name(), line[4]);
    assertEquals(datasetSearchResult.getSubtype().name(), line[5]);
    assertEquals(datasetSearchResult.getHostingOrganizationKey().toString(), line[6]);
    assertEquals(datasetSearchResult.getHostingOrganizationTitle(), line[7]);
    assertEquals(datasetSearchResult.getHostingCountry().getIso2LetterCode(), line[8]);
    assertEquals(datasetSearchResult.getPublishingOrganizationKey().toString(), line[9]);
    assertEquals(datasetSearchResult.getPublishingOrganizationTitle(), line[10]);
    assertEquals(datasetSearchResult.getPublishingCountry().getIso2LetterCode(), line[11]);
    assertEquals(datasetSearchResult.getEndorsingNodeKey().toString(), line[12]);
    assertTrue(
        datasetSearchResult
            .getNetworkKeys()
            .containsAll(
                Arrays.stream(line[13].split(CsvWriter.ARRAY_DELIMITER))
                    .map(UUID::fromString)
                    .collect(Collectors.toList())));
    assertEquals(datasetSearchResult.getProjectIdentifier(), line[14]);
    assertEquals(datasetSearchResult.getRecordCount(), Integer.parseInt(line[15]));
    // Last characters has carriage return \r
    assertEquals(
        datasetSearchResult.getNameUsagesCount(), Integer.parseInt(line[16].replace("\r", "")));
  }

  @Test
  public void datasetSearchTest() {

    // Test data
    DatasetSearchResult datasetSearchResult1 = newDatasetSearchResult(1);
    DatasetSearchResult datasetSearchResult2 = newDatasetSearchResult(2);

    List<DatasetSearchResult> datasets = Arrays.asList(datasetSearchResult1, datasetSearchResult2);

    StringWriter writer = new StringWriter();

    CsvWriter<DatasetSearchResult> csvWriter =
        CsvWriter.datasetSearchResultCsvWriter(datasets, ExportFormat.CSV);
    csvWriter.export(writer);

    assertExport(datasets, writer, csvWriter, this::assertDatasetSearchResult);
  }

  /**
   * Test one DatasetOccurrenceDownloadUsage against its expected exported data.
   */
  private void assertDatasetOccurrenceDownloadUsage(
      DatasetOccurrenceDownloadUsage downloadUsage, String[] line) {
    assertEquals(downloadUsage.getDatasetDOI().toString(), line[0]);
    assertEquals(downloadUsage.getDatasetKey().toString(), line[1]);
    assertEquals(downloadUsage.getDatasetTitle(), line[2]);
    assertEquals(downloadUsage.getDatasetCitation(), line[3]);
    assertEquals(downloadUsage.getNumberRecords(), Long.parseLong(line[4].replace("\r", "")));
  }

  /**
   * Generates test instances of DatasetOccurrenceDownloadUsage.
   */
  private static DatasetOccurrenceDownloadUsage newDatasetOccurrenceDownloadUsageTest(
      int consecutive) {
    DatasetOccurrenceDownloadUsage downloadUsage = new DatasetOccurrenceDownloadUsage();

    downloadUsage.setDatasetKey(UUID.randomUUID());
    downloadUsage.setDatasetDOI(new DOI("10.21373/6m9yw" + consecutive));
    downloadUsage.setDatasetTitle("UsageTitle" + consecutive);
    downloadUsage.setDatasetCitation("Citation" + consecutive);
    downloadUsage.setNumberRecords(consecutive);

    return downloadUsage;
  }

  @Test
  public void datasetOccurrenceDownloadUsageTest() {

    // Test data
    List<DatasetOccurrenceDownloadUsage> downloadUsages =
        Arrays.asList(
            newDatasetOccurrenceDownloadUsageTest(1), newDatasetOccurrenceDownloadUsageTest(2));

    StringWriter writer = new StringWriter();

    CsvWriter<DatasetOccurrenceDownloadUsage> csvWriter =
        CsvWriter.datasetOccurrenceDownloadUsageCsvWriter(downloadUsages, ExportFormat.CSV);
    csvWriter.export(writer);

    assertExport(downloadUsages, writer, csvWriter, this::assertDatasetOccurrenceDownloadUsage);
  }

  /**
   * Generates test instances of CollectionView.
   */
  @SneakyThrows
  private static CollectionView newCollectionView(int consecutive) {
    CollectionView collectionView = new CollectionView();

    collectionView.setInstitutionCode("INST" + consecutive);
    collectionView.setInstitutionName("INST" + consecutive);

    Address address = new Address();
    address.setAddress("Universitetsparken 15");
    address.setCity("Copenhagen");
    address.setCountry(Country.DENMARK);
    address.setPostalCode("2100");
    address.setProvince("Zealand");
    address.setKey(consecutive);

    Collection collection = new Collection();
    collection.setInstitutionKey(UUID.randomUUID());
    collection.setEmail(Collections.singletonList("ints" + consecutive + "@gbif.org"));
    collection.setPhone(Collections.singletonList("1234" + consecutive));
    collection.setAddress(address);

    AlternativeCode alternativeCode = new AlternativeCode();
    alternativeCode.setCode("alt" + consecutive);
    alternativeCode.setDescription("altDescription" + consecutive);
    collection.setAlternativeCodes(Collections.singletonList(alternativeCode));
    collection.setApiUrl(new URI("http://coll" + consecutive + ".org"));
    collection.setCatalogUrl(new URI("http://cat" + consecutive + ".org"));
    collection.setCode("COL" + consecutive);
    collection.setContentTypes(
        Collections.singletonList(CollectionContentType.ARCHAEOLOGICA_WOODEN_ARTIFACTS));
    collection.setCreatedBy("me");
    collection.setCreated(new Date());
    collection.setModifiedBy("me");
    collection.setModified(new Date());
    collection.setDescription("Collections description" + consecutive);
    collection.setGeography("Geo" + consecutive);
    collection.setHomepage(new URI("http://coll" + consecutive + ".org"));
    collection.setImportantCollectors(Collections.singletonList("Collector" + consecutive));
    collection.setIncorporatedCollections(Collections.singletonList("Coll1." + consecutive));
    collection.setKey(UUID.randomUUID());
    collection.setName("Collection" + consecutive);
    collection.setMailingAddress(address);
    collection.setPreservationTypes(Collections.singletonList(PreservationType.SAMPLE_DRIED));
    collection.setTaxonomicCoverage("world");

    collection.setActive(true);
    Comment comment = new Comment();
    comment.setContent("Comment" + consecutive);
    comment.setKey(consecutive);
    collection.setComments(Collections.singletonList(comment));

    collection.setDoi(new DOI("10.21373/6m9yw" + consecutive));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier" + consecutive);
    identifier.setType(IdentifierType.LSID);
    identifier.setKey(consecutive);
    collection.setIdentifiers(Collections.singletonList(identifier));

    MachineTag machineTag = new MachineTag();
    machineTag.setName("gbif");
    machineTag.setName("collections");
    machineTag.setValue("v" + consecutive);
    machineTag.setKey(consecutive);
    collection.setMachineTags(Collections.singletonList(machineTag));

    collection.setNumberSpecimens(consecutive);

    Tag tag = new Tag();
    tag.setValue("tag" + consecutive);
    tag.setKey(consecutive);
    collection.setTags(Collections.singletonList(tag));

    collection.setAccessionStatus(AccessionStatus.INSTITUTIONAL);
    collection.setCollectionSummary(Collections.singletonMap("count", consecutive));
    collection.setIndexHerbariorumRecord(false);
    collection.setPersonalCollection(false);
    collection.setReplacedBy(UUID.randomUUID());
    collection.setNotes("Note" + consecutive);
    collectionView.setCollection(collection);

    return collectionView;
  }

  @Test
  public void collectionsTest() {

    // Test data
    CollectionView collectionView1 = newCollectionView(1);
    CollectionView collectionView2 = newCollectionView(2);

    List<CollectionView> collections = Arrays.asList(collectionView1, collectionView2);

    StringWriter writer = new StringWriter();

    CsvWriter<CollectionView> csvWriter = CsvWriter.collections(collections, ExportFormat.CSV);
    csvWriter.export(writer);

    assertExport(collections, writer, csvWriter, this::assertCollection);
  }

  /**
   * Test one CollectionView against its expected exported data.
   */
  private void assertCollection(CollectionView collectionView, String[] line) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(StdDateFormat.DATE_FORMAT_STR_ISO8601);
    assertEquals(collectionView.getCollection().getKey().toString(), line[0]);
    assertEquals(collectionView.getCollection().getCode(), line[1]);
    assertEquals(collectionView.getCollection().getName(), line[2]);
    assertEquals(collectionView.getCollection().getDescription(), line[3]);
    assertEquals(
        collectionView.getCollection().getAddress().getCountry().getIso2LetterCode(), line[4]);
    assertEquals(
        CsvWriter.ListCollectionContentTypeProcessor.toString(
            collectionView.getCollection().getContentTypes()),
        line[5]); //
    assertEquals(collectionView.getCollection().isActive(), Boolean.parseBoolean(line[6]));
    assertEquals(
        collectionView.getCollection().isPersonalCollection(), Boolean.parseBoolean(line[7]));
    assertEquals(collectionView.getCollection().getDoi().toString(), line[8]);
    assertEquals(
        CsvWriter.ListStringProcessor.toString(collectionView.getCollection().getEmail()), line[9]);
    assertEquals(
        CsvWriter.ListStringProcessor.toString(collectionView.getCollection().getPhone()),
        line[10]);
    assertEquals(collectionView.getCollection().getHomepage().toString(), line[11]);
    assertEquals(collectionView.getCollection().getCatalogUrl().toString(), line[12]);
    assertEquals(collectionView.getCollection().getApiUrl().toString(), line[13]);
    assertEquals(
        CsvWriter.ListPreservationTypeProcessor.toString(
            collectionView.getCollection().getPreservationTypes()),
        line[14]);
    assertEquals(collectionView.getCollection().getAccessionStatus().name(), line[15]);
    assertEquals(collectionView.getInstitutionName(), line[16]);
    assertEquals(collectionView.getInstitutionCode(), line[17]);
    assertEquals(collectionView.getCollection().getInstitutionKey().toString(), line[18]);
    assertEquals(
        CsvWriter.AddressProcessor.toString(collectionView.getCollection().getMailingAddress()),
        line[19]);
    assertEquals(
        CsvWriter.AddressProcessor.toString(collectionView.getCollection().getAddress()), line[20]);
    assertEquals(collectionView.getCollection().getCreatedBy(), line[21]);
    assertEquals(collectionView.getCollection().getModifiedBy(), line[22]);
    assertEquals(dateFormat.format(collectionView.getCollection().getCreated()), line[23]);
    assertEquals(dateFormat.format(collectionView.getCollection().getModified()), line[24]);
    assertEquals(
        Optional.ofNullable(collectionView.getCollection().getDeleted())
            .map(dateFormat::format)
            .orElse(""),
        line[25]);
    assertEquals(
        CsvWriter.ListTagsProcessor.toString(collectionView.getCollection().getTags()), line[26]);
    assertEquals(
        CsvWriter.ListIdentifierProcessor.toString(collectionView.getCollection().getIdentifiers()),
        line[27]);
    assertEquals(
        CsvWriter.ListContactProcessor.toString(collectionView.getCollection().getContactPersons()),
        line[28]);
    assertEquals(
        collectionView.getCollection().isIndexHerbariorumRecord(), Boolean.parseBoolean(line[29]));
    assertEquals(collectionView.getCollection().getNumberSpecimens(), Integer.parseInt(line[30]));
    assertEquals(
        CsvWriter.ListMachineTagProcessor.toString(collectionView.getCollection().getMachineTags()),
        line[31]);
    assertEquals(collectionView.getCollection().getTaxonomicCoverage(), line[32]);
    assertEquals(collectionView.getCollection().getGeography(), line[33]);
    assertEquals(collectionView.getCollection().getNotes(), line[34]);
    assertEquals(
        CsvWriter.ListStringProcessor.toString(
            collectionView.getCollection().getIncorporatedCollections()),
        line[35]);
    assertEquals(
        CsvWriter.ListStringProcessor.toString(
            collectionView.getCollection().getImportantCollectors()),
        line[36]);
    assertEquals(
        CsvWriter.CollectionSummaryProcessor.toString(
            collectionView.getCollection().getCollectionSummary()),
        line[37]);
    assertEquals(
        CsvWriter.ListAlternativeCodeProcessor.toString(
            collectionView.getCollection().getAlternativeCodes()),
        line[38]);
    assertEquals(
        CsvWriter.ListCommentProcessor.toString(collectionView.getCollection().getComments()),
        line[39]);
    assertEquals(
        CsvWriter.ListOccurrenceMappingsProcessor.toString(
            collectionView.getCollection().getOccurrenceMappings()),
        line[40]);
    assertEquals(
        collectionView.getCollection().getReplacedBy().toString(), line[41].replace("\r", ""));
  }

  /**
   * Generates test instances of Institution.
   */
  @SneakyThrows
  private static Institution newInstitution(int consecutive) {
    Institution institution = new Institution();

    Address address = new Address();
    address.setAddress("Universitetsparken 15");
    address.setCity("Copenhagen");
    address.setPostalCode("2100");
    address.setProvince("Zealand");
    address.setKey(consecutive);

    institution.setKey(UUID.randomUUID());
    institution.setEmail(Collections.singletonList("ints" + consecutive + "@gbif.org"));
    institution.setPhone(Collections.singletonList("1234" + consecutive));
    institution.setAddress(address);

    AlternativeCode alternativeCode = new AlternativeCode();
    alternativeCode.setCode("ALT_INST" + consecutive);
    alternativeCode.setDescription("alternative description" + consecutive);
    institution.setAlternativeCodes(Collections.singletonList(alternativeCode));
    institution.setApiUrl(new URI("http://api.inst" + consecutive + ".org"));
    institution.setCatalogUrl(new URI("http://cat.inst" + consecutive + ".org"));
    institution.setLogoUrl(new URI("http://inst" + consecutive + ".org/log.png"));
    institution.setHomepage(new URI("http://inst" + consecutive + ".org/l"));
    institution.setCode("INST" + consecutive);
    institution.setAdditionalNames(Collections.singletonList("Additional name" + consecutive));
    institution.setDisciplines(Collections.singletonList(Discipline.SPACE));
    institution.setConvertedToCollection(UUID.randomUUID());
    institution.setLatitude(new BigDecimal(40));
    institution.setLongitude(new BigDecimal(90));
    institution.setType(InstitutionType.BOTANICAL_GARDEN);
    institution.setCitesPermitNumber("permit" + consecutive);
    institution.setTaxonomicDescription("Taxa description " + consecutive);
    institution.setInstitutionalGovernance(InstitutionGovernance.ACADEMIC_NON_PROFIT);
    institution.setCreatedBy("me");
    institution.setCreated(new Date());
    institution.setModifiedBy("me");
    institution.setModified(new Date());
    institution.setDescription("Institution description" + consecutive);
    institution.setGeographicDescription("Geo description" + consecutive);
    institution.setHomepage(new URI("http://coll" + consecutive + ".org"));
    institution.setFoundingDate(new Date());

    Address mailingAddress = new Address();
    mailingAddress.setAddress("Universitetsparken 15");
    mailingAddress.setCity("Copenhagen");
    mailingAddress.setPostalCode("2100");
    mailingAddress.setProvince("Zealand");
    mailingAddress.setCountry(Country.DENMARK);
    mailingAddress.setKey(consecutive);
    institution.setMailingAddress(mailingAddress);

    institution.setKey(UUID.randomUUID());
    institution.setName("Collection" + consecutive);

    institution.setActive(true);
    Comment comment = new Comment();
    comment.setContent("Comment" + consecutive);
    comment.setKey(consecutive);
    institution.setComments(Collections.singletonList(comment));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier" + consecutive);
    identifier.setType(IdentifierType.LSID);
    identifier.setKey(consecutive);
    institution.setIdentifiers(Collections.singletonList(identifier));

    MachineTag machineTag = new MachineTag();
    machineTag.setName("gbif");
    machineTag.setName("institutions");
    machineTag.setValue("v" + consecutive);
    machineTag.setKey(consecutive);
    institution.setMachineTags(Collections.singletonList(machineTag));

    institution.setNumberSpecimens(consecutive);

    Tag tag = new Tag();
    tag.setValue("tag" + consecutive);
    tag.setKey(consecutive);
    institution.setTags(Collections.singletonList(tag));

    institution.setIndexHerbariorumRecord(false);
    institution.setReplacedBy(UUID.randomUUID());

    return institution;
  }

  @Test
  public void institutionsTest() {

    // Test data
    Institution institution1 = newInstitution(1);
    Institution institution2 = newInstitution(2);

    List<Institution> institutions = Arrays.asList(institution1, institution2);

    StringWriter writer = new StringWriter();

    CsvWriter<Institution> csvWriter = CsvWriter.institutions(institutions, ExportFormat.CSV);
    csvWriter.export(writer);

    assertExport(institutions, writer, csvWriter, this::assertInstitution);
  }

  /**
   * Test one Institution against its expected exported data.
   */
  private void assertInstitution(Institution institution, String[] line) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(StdDateFormat.DATE_FORMAT_STR_ISO8601);

    assertEquals(institution.getKey().toString(), line[0]);
    assertEquals(institution.getCode(), line[1]);
    assertEquals(institution.getName(), line[2]);
    assertEquals(institution.getDescription(), line[3]);
    assertEquals(institution.getMailingAddress().getCountry().getIso2LetterCode(), line[4]);
    assertEquals(institution.getType().name(), line[5]); //
    assertEquals(institution.isActive(), Boolean.parseBoolean(line[6]));
    assertEquals(CsvWriter.ListStringProcessor.toString(institution.getEmail()), line[7]);
    assertEquals(CsvWriter.ListStringProcessor.toString(institution.getPhone()), line[8]);
    assertEquals(institution.getHomepage().toString(), line[9]);
    assertEquals(institution.getCatalogUrl().toString(), line[10]);
    assertEquals(institution.getApiUrl().toString(), line[11]);
    assertEquals(institution.getInstitutionalGovernance().name(), line[12]);
    assertEquals(
        CsvWriter.ListDisciplinesProcessor.toString(institution.getDisciplines()), line[13]);
    assertEquals(institution.getLatitude().toString(), line[14]);
    assertEquals(institution.getLongitude().toString(), line[15]);
    assertEquals(CsvWriter.AddressProcessor.toString(institution.getMailingAddress()), line[16]);
    assertEquals(CsvWriter.AddressProcessor.toString(institution.getAddress()), line[17]);
    assertEquals(
        CsvWriter.ListStringProcessor.toString(institution.getAdditionalNames()), line[18]);
    assertEquals(
        Optional.ofNullable(institution.getFoundingDate()).map(dateFormat::format).orElse(""),
        line[19]);
    assertEquals(institution.getGeographicDescription(), line[20]);
    assertEquals(institution.getTaxonomicDescription(), line[21]);
    assertEquals(Integer.toString(institution.getNumberSpecimens()), line[22]);
    assertEquals(institution.isIndexHerbariorumRecord(), Boolean.parseBoolean(line[23]));
    assertEquals(institution.getLogoUrl().toString(), line[24]);
    assertEquals(institution.getCitesPermitNumber(), line[25]);
    assertEquals(institution.getCreatedBy(), line[26]);
    assertEquals(institution.getModifiedBy(), line[27]);
    assertEquals(dateFormat.format(institution.getCreated()), line[28]);
    assertEquals(dateFormat.format(institution.getModified()), line[29]);
    assertEquals(
        Optional.ofNullable(institution.getDeleted()).map(dateFormat::format).orElse(""), line[30]);
    assertEquals(CsvWriter.ListTagsProcessor.toString(institution.getTags()), line[31]);
    assertEquals(
        CsvWriter.ListIdentifierProcessor.toString(institution.getIdentifiers()), line[32]);
    assertEquals(CsvWriter.ListContactProcessor.toString(institution.getContactPersons()), line[33]);
    assertEquals(
        CsvWriter.ListMachineTagProcessor.toString(institution.getMachineTags()), line[34]);
    assertEquals(
        CsvWriter.ListAlternativeCodeProcessor.toString(institution.getAlternativeCodes()),
        line[35]);
    assertEquals(CsvWriter.ListCommentProcessor.toString(institution.getComments()), line[36]);
    assertEquals(
        CsvWriter.ListOccurrenceMappingsProcessor.toString(institution.getOccurrenceMappings()),
        line[37]);
    assertEquals(institution.getReplacedBy().toString(), line[38]);
    assertEquals(institution.getConvertedToCollection().toString(), line[39].replace("\r", ""));
  }
}
