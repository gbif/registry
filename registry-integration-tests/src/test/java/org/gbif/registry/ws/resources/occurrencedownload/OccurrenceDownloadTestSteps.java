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
package org.gbif.registry.ws.resources.occurrencedownload;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.fixtures.TestConstants;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.utils.matcher.RegistryMatchers.isDownloadDoi;
import static org.gbif.registry.utils.matcher.RegistryMatchers.isRegistryOffsetDateTimeFormat;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OccurrenceDownloadTestSteps {

  private ResultActions result;
  private MockMvc mvc;
  private DOI doi;
  private Download download;

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private OccurrenceDownloadService occurrenceDownloadService;

  @Autowired private DataSource ds;

  private Connection connection;

  @Autowired private BeanUtilsBean beanUtils;

  @Before("@OccurrenceDownload")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource("/scripts/occurrencedownload/occurrence_download_list_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Before("@DownloadUserStatistic")
  public void prepareUserStatistic() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource(
            "/scripts/occurrencedownload/occurrence_download_statistic_clean.sql"));
    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource(
            "/scripts/occurrencedownload/occurrence_download_statistic_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Before("@OccurrenceDownloadUsage")
  public void prepareDownloadUsage() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource("/scripts/occurrencedownload/occurrence_download_usage_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@OccurrenceDownload")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource("/scripts/occurrencedownload/occurrence_download_cleanup.sql"));

    connection.close();
  }

  @After("@DownloadUserStatistic")
  public void cleanUserStatistic() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource(
            "/scripts/occurrencedownload/occurrence_download_statistic_clean.sql"));
  }

  @After("@OccurrenceDownloadUsage")
  public void cleanDownloadUsage() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource("/scripts/occurrencedownload/occurrence_download_usage_clean.sql"));
  }

  @Given("equals predicate download")
  public void prepareEqualsPredicateDownload() {
    this.download = getTestInstancePredicateDownload();
  }

  private Download getTestInstancePredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
        new PredicateDownloadRequest(
            new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "212"),
            TestConstants.TEST_ADMIN,
            Arrays.asList("address1@mailinator.org", "address2@mailinator.org"),
            true,
            DownloadFormat.DWCA));
    return download;
  }

  @Given("download with invalid parameters")
  public void prepareInvalidParamsDownload(DataTable dataTable) throws Exception {
    DateTimeConverter dateConverter = new DateConverter(null);
    dateConverter.setPatterns(new String[] {"dd-MM-yyyy"});
    ConvertUtils.register(dateConverter, Date.class);

    for (Map<String, String> params : dataTable.asMaps()) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        BeanUtils.setProperty(
            download, entry.getKey(), "null".equals(entry.getValue()) ? null : entry.getValue());
      }
    }
  }

  @Given("null predicate download")
  public void prepareNullPredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
        new PredicateDownloadRequest(
            null,
            TestConstants.TEST_ADMIN,
            Arrays.asList("address1@mailinator.org", "address2@mailinator.org"),
            true,
            DownloadFormat.DWCA));
    this.download = download;
  }

  @Given("{int} occurrence download(s)")
  public void preparePredicateDownloadsForList(int numberOfDownloads, DataTable dataTable) {
    assertEquals(numberOfDownloads, dataTable.asMaps().size());
    // prepared by scripts in @Before
  }

  @Given("{int} dataset(s)")
  public void prepareDatasets(int numberOfDatasets, DataTable dataTable) {
    assertEquals(numberOfDatasets, dataTable.asMaps().size());
    // prepared by scripts in @Before
  }

  @Given("{int} download statistic record(s)")
  public void prepareDownloadStatistic(int numberOfObjects, DataTable dataTable) {
    assertEquals(numberOfObjects, dataTable.asMaps().size());
    // prepared by scripts in @Before
  }

  @Given("{int} download user statistic record(s)")
  public void prepareDownloadUserStatistic(int numberOfObjects, DataTable dataTable) {
    assertEquals(numberOfObjects, dataTable.asMaps().size());
    // prepared by scripts in @Before
  }

  @When("create download using {word} {string}")
  public void createDownload(String userRole, String username) throws Exception {
    String stringContent = objectMapper.writeValueAsString(download);

    result =
        mvc.perform(
            post("/occurrence/download")
                .with(httpBasic(username, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(stringContent));
  }

  @When("get download")
  public void getDownload() throws Exception {
    getDownload(download.getKey());
  }

  @When("get download {string}")
  public void getDownload(String downloadKey) throws Exception {
    result = mvc.perform(get("/occurrence/download/{key}", downloadKey));
  }

  @When("get download by doi")
  public void getDownloadByDoi() throws Exception {
    result =
        mvc.perform(
            get("/occurrence/download/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix()));
  }

  @When("list downloads using {word} {string}")
  public void listDownloads(String userRole, String username) throws Exception {
    listDownloads(userRole, username, new HashMap<>());
  }

  @When("list downloads using {word} {string} with query params")
  public void listDownloads(String userRole, String username, Map<String, List<String>> params)
      throws Exception {
    result =
        mvc.perform(
            get("/occurrence/download")
                .with(httpBasic(username, TEST_PASSWORD))
                .params(new LinkedMultiValueMap<>(params)));
  }

  @When("list downloads by user {string} using {word} {string}")
  public void listDownloadsByUser(String userParam, String userRole, String username)
      throws Exception {
    listDownloadsByUser(userParam, userRole, username, new HashMap<>());
  }

  @When("list downloads by user {string} using {word} {string} with query params")
  public void listDownloadsByUser(
      String userParam, String userRole, String username, Map<String, List<String>> params)
      throws Exception {
    result =
        mvc.perform(
            get("/occurrence/download/user/{user}", userParam)
                .with(httpBasic(username, TEST_PASSWORD))
                .params(new LinkedMultiValueMap<>(params)));
  }

  @When("update occurrence download {string} using {word} {string} with values")
  public void updateDownload(
      String downloadKey, String userRole, String username, Map<String, String> params)
      throws Exception {
    download = occurrenceDownloadService.get(downloadKey);

    for (Map.Entry<String, String> entry : params.entrySet()) {
      beanUtils.setProperty(download, entry.getKey(), entry.getValue());
    }

    String content = objectMapper.writeValueAsString(download);

    result =
        mvc.perform(
            put("/occurrence/download/{key}", downloadKey)
                .with(httpBasic(username, TEST_PASSWORD))
                .content(content)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("get download statistic using {word} {string} with params")
  public void getDownloadStatisticByCountry(
      String userType, String username, Map<String, List<String>> requestParams) throws Exception {
    result =
        mvc.perform(
                get("/occurrence/download/statistics/downloadsByUserCountry")
                    .with(httpBasic(username, TEST_PASSWORD))
                    .params(new LinkedMultiValueMap<>(requestParams)))
            .andDo(print());
  }

  @When("get download statistic using {word} {string} without params")
  public void getDownloadStatisticByCountry(String userType, String username) throws Exception {
    getDownloadStatisticByCountry(userType, username, new LinkedHashMap<>());
  }

  @When("get downloaded records by dataset using {word} {string} with params")
  public void getDownloadedRecordsByDataset(
      String userType, String username, Map<String, List<String>> requestParams) throws Exception {
    result =
        mvc.perform(
                get("/occurrence/download/statistics/downloadedRecordsByDataset")
                    .with(httpBasic(username, TEST_PASSWORD))
                    .params(new LinkedMultiValueMap<>(requestParams)))
            .andDo(print());
  }

  @When("get downloaded records by dataset using {word} {string} without params")
  public void getDownloadedRecordsByDataset(String userType, String username) throws Exception {
    getDownloadedRecordsByDataset(userType, username, new LinkedHashMap<>());
  }

  @When(
      "create occurrence download usage for download {string} using {word} {string} with citations")
  public void createOccurrenceDownloadUsage(
      String downloadKey, String userType, String username, Map<String, String> citations)
      throws Exception {
    // values must be numbers not strings
    Map<String, Long> mappedCitations = new HashMap<>();
    for (String key : citations.keySet()) {
      mappedCitations.put(key, Long.valueOf(citations.get(key)));
    }
    String stringContent = objectMapper.writeValueAsString(mappedCitations);

    result =
        mvc.perform(
            post("/occurrence/download/{key}/datasets", downloadKey)
                .with(httpBasic(username, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(stringContent));
  }

  @When("list dataset usages by dataset {string} using {word} {string}")
  public void listUsagesByDataset(String datasetKey, String userType, String username)
      throws Exception {
    result =
        mvc.perform(
            get("/occurrence/download/dataset/{datasetKey}", datasetKey)
                .with(httpBasic(username, TEST_PASSWORD)));
  }

  @When("list dataset usages for download {string} using {word} {string}")
  public void listUsages(String downloadKey, String userType, String username) throws Exception {
    result =
        mvc.perform(
            get("/occurrence/download/{key}/datasets", downloadKey)
                .with(httpBasic(username, TEST_PASSWORD)));
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("{word} download assertions passed")
  public void checkDownloadResponse(String downloadType, Map<String, String> params)
      throws Exception {
    checkDownloadResponse(params);
  }

  @Then("download assertions passed")
  public void checkDownloadResponse(Map<String, String> params) throws Exception {
    result
        .andExpect(jsonPath("$.key").value(download.getKey()))
        .andExpect(jsonPath("$.doi").value(isDownloadDoi()))
        .andExpect(jsonPath("$.created").value(isRegistryOffsetDateTimeFormat()))
        .andExpect(jsonPath("$.modified").value(isRegistryOffsetDateTimeFormat()))
        .andExpect(jsonPath("$.eraseAfter").value(isRegistryOffsetDateTimeFormat()))
        // sensitive information is not present
        .andExpect(jsonPath("$.request.creator").doesNotExist())
        .andExpect(jsonPath("$.request.notificationAddresses").doesNotExist());

    for (Map.Entry<String, String> entry : params.entrySet()) {
      result.andExpect(jsonPath("$." + entry.getKey()).value(entry.getValue()));
    }
  }

  @Then("extract doi from download")
  public void extractDoiFromDownload() {
    doi = occurrenceDownloadService.get(this.download.getKey()).getDoi();
  }

  @Then("download creation error response is")
  public void checkDownloadCreationErrorResponse(DataTable dataTable) throws Exception {
    int iteration = 0;
    for (Map<String, String> params : dataTable.asMaps()) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        result.andExpect(xpath(String.format("/ul/li[%d]", ++iteration)).string(entry.getValue()));
      }
      iteration = 0;
    }
  }

  @Then("{int} downloads in occurrence downloads list response")
  public void checkOccurrenceDownloadList(int numberOrRecords) throws Exception {
    result.andExpect(jsonPath("$.count").value(numberOrRecords));
  }

  @Then("response contains {int} records for {int} years")
  public void checkStatisticResponse(
      int numberOfRecords, int expectedYears, List<Map<String, String>> expectedData)
      throws Exception {
    result.andExpect(jsonPath("$.length()").value(expectedYears));
    for (Map<String, String> expected : expectedData) {
      result.andExpect(jsonPath("$." + expected.get("year.month")).value(expected.get("value")));
    }
  }

  @Then("occurrence downloads usage list contains {int} elements")
  public void checkDownloadUsageResponse(int expectedNumberOfElements) throws Exception {
    result.andExpect(jsonPath("$.count").value(expectedNumberOfElements));
  }

  @Then("occurrence downloads usage list of {int} elements is")
  public void checkDownloadUsageResponse(int expectedNumberOfElements, DataTable dataTable)
      throws Exception {
    result.andExpect(jsonPath("$.count").value(expectedNumberOfElements));
    int iteration = 0;
    for (Map<String, String> params : dataTable.asMaps()) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        result.andExpect(
            jsonPath(String.format("$.results[%d].%s", iteration, entry.getKey()))
                .value(entry.getValue()));
      }
      iteration++;
    }
  }

  /**
   * Creates {@link Download} instance with test data using a predicate request. The key is
   * generated randomly using the class java.util.UUID. The instance generated should be ready and
   * valid to be persisted.
   */
  private static Download getTestInstanceDownload() {
    Download download = new Download();
    download.setKey(UUID.randomUUID().toString());
    download.setStatus(Download.Status.PREPARING);
    download.setDoi(new DOI("doi:10.1234/1ASCDU"));
    download.setDownloadLink("testUrl");
    download.setEraseAfter(Date.from(OffsetDateTime.now(ZoneOffset.UTC).plusMonths(6).toInstant()));

    return download;
  }
}
