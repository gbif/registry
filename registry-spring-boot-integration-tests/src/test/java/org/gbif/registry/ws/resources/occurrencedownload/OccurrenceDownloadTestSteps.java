package org.gbif.registry.ws.resources.occurrencedownload;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.SqlDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.fixtures.TestConstants;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.gbif.registry.utils.matcher.RegistryMatchers.isDownloadDoi;
import static org.gbif.registry.utils.matcher.RegistryMatchers.isRegistryDateFormat;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OccurrenceDownloadTestSteps {

  private ResultActions result;
  private MockMvc mvc;
  private DOI doi;
  private Download download;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private OccurrenceDownloadService occurrenceDownloadService;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Before("@OccurrenceDownload")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @Before("@OccurrenceDownloadList")
  public void prepareDataForList() throws Exception {
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/occurrencedownload/occurrence_download_list_prepare.sql"));
  }

  @After("@OccurrenceDownload")
  public void tearDown() throws Exception {
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/occurrencedownload/occurrence_download_cleanup.sql"));

    connection.close();
  }

  @Given("equals predicate download")
  public void prepareEqualsPredicateDownload() {
    this.download = getTestInstancePredicateDownload();
  }

  private Download getTestInstancePredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
      new PredicateDownloadRequest(new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "212"),
        TestConstants.TEST_ADMIN, Collections.singleton("downloadtest@gbif.org"),
        true, DownloadFormat.DWCA));
    return download;
  }

  @Given("download with invalid parameters")
  public void prepareInvalidParamsDownload(DataTable dataTable) throws Exception {
    DateTimeConverter dateConverter = new DateConverter(null);
    dateConverter.setPatterns(new String[]{"dd-MM-yyyy"});
    ConvertUtils.register(dateConverter, Date.class);

    for (Map<String, String> params : dataTable.asMaps()) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        BeanUtils.setProperty(download, entry.getKey(), "null".equals(entry.getValue()) ? null : entry.getValue());
      }
    }
  }

  @Given("null predicate download")
  public void prepareNullPredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
      new PredicateDownloadRequest(null,
        TestConstants.TEST_ADMIN, Collections.singleton("downloadtest@gbif.org"),
        true, DownloadFormat.DWCA));
    this.download = download;
  }

  @Given("sql download")
  public void prepareSqlDownload() {
    this.download = getTestInstanceSqlDownload();
  }

  private Download getTestInstanceSqlDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(new SqlDownloadRequest("SELECT datasetKey FROM occurrence", TestConstants.TEST_ADMIN,
      Collections.singleton("downloadtest@gbif.org"), true));
    return download;
  }

  @Given("null sql download")
  public void prepareNullSqlDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(new SqlDownloadRequest(null, TestConstants.TEST_ADMIN,
      Collections.singleton("downloadtest@gbif.org"), true));
    this.download = download;
  }

  @Given("{int} predicate downloads")
  public void preparePredicateDownloadsForList(int numberOfDownloads) throws Exception {
    // prepared by scripts in @Before
  }

  @Given("{int} sql downloads")
  public void prepareSqlDownloadsForList(int numberOfDownloads) throws Exception {
    // prepared by scripts in @Before
  }

  @When("create download by {word} {string}")
  public void createDownload(String userRole, String username) throws Exception {
    String stringContent = objectMapper.writeValueAsString(download);

    result = mvc
      .perform(
        post("/occurrence/download")
          .with(httpBasic(username, TEST_PASSWORD))
          .contentType(MediaType.APPLICATION_JSON)
          .content(stringContent))
      .andDo(print());
  }

  @When("get download")
  public void getDownload() throws Exception {
    result = mvc
      .perform(
        get("/occurrence/download/{key}", download.getKey()))
      .andDo(print());
  }

  @When("get download by doi")
  public void getDownloadByDoi() throws Exception {
    result = mvc
      .perform(
        get("/occurrence/download/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix()))
      .andDo(print());
  }

  @When("list downloads by {word} {string}")
  public void listDownloads(String userRole, String username) throws Exception {
    listDownloads(userRole, username, new HashMap<>());
  }

  @When("list downloads by {word} {string} with query params")
  public void listDownloads(String userRole, String username, Map<String, List<String>> params) throws Exception {
    result = mvc
      .perform(
        get("/occurrence/download")
          .with(httpBasic(username, TEST_PASSWORD))
          .params(new LinkedMultiValueMap<>(params))
      )
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("equals predicate download assertions passed")
  public void checkGetDownloadResponse(DataTable dataTable) throws Exception {
    for (Map<String, String> params : dataTable.asMaps()) {
      result
        .andExpect(jsonPath("$.key").value(download.getKey()))
        .andExpect(jsonPath("$.doi").value(isDownloadDoi()))
        .andExpect(jsonPath("$.request.predicate.type").value(params.get("predicateType")))
        .andExpect(jsonPath("$.request.predicate.key").value(params.get("predicateKey")))
        .andExpect(jsonPath("$.request.predicate.value").value(params.get("predicateValue")))
        .andExpect(jsonPath("$.request.sendNotification").value(params.get("sendNotification")))
        .andExpect(jsonPath("$.request.format").value(params.get("format")))
        .andExpect(jsonPath("$.created").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.modified").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.eraseAfter").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.status").value(params.get("status")))
        .andExpect(jsonPath("$.downloadLink").value(params.get("downloadLink")))
        .andExpect(jsonPath("$.size").value(params.get("size")))
        .andExpect(jsonPath("$.totalRecords").value(params.get("totalRecords")))
        .andExpect(jsonPath("$.numberDatasets").value(params.get("numberDatasets")));
    }
  }

  @Then("null predicate download assertions passed")
  public void checkGetNullPredicateDownloadResponse(DataTable dataTable) throws Exception {
    for (Map<String, String> params : dataTable.asMaps()) {
      result
        .andExpect(jsonPath("$.key").value(download.getKey()))
        .andExpect(jsonPath("$.doi").value(isDownloadDoi()))
        .andExpect(jsonPath("$.request.sendNotification").value(params.get("sendNotification")))
        .andExpect(jsonPath("$.request.format").value(params.get("format")))
        .andExpect(jsonPath("$.created").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.modified").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.eraseAfter").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.status").value(params.get("status")))
        .andExpect(jsonPath("$.downloadLink").value(params.get("downloadLink")))
        .andExpect(jsonPath("$.size").value(params.get("size")))
        .andExpect(jsonPath("$.totalRecords").value(params.get("totalRecords")))
        .andExpect(jsonPath("$.numberDatasets").value(params.get("numberDatasets")));
    }
  }

  @Then("sql download assertions passed")
  public void checkGetSqlDownloadResponse(DataTable dataTable) throws Exception {
    for (Map<String, String> params : dataTable.asMaps()) {
      result
        .andExpect(jsonPath("$.license").value(params.get("license")))
        .andExpect(jsonPath("$.key").value(download.getKey()))
        .andExpect(jsonPath("$.doi").value(isDownloadDoi()))
        .andExpect(jsonPath("$.request.sql").value(params.get("sql")))
        .andExpect(jsonPath("$.request.sendNotification").value(params.get("sendNotification")))
        .andExpect(jsonPath("$.request.format").value(params.get("format")))
        .andExpect(jsonPath("$.created").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.modified").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.eraseAfter").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.status").value(params.get("status")))
        .andExpect(jsonPath("$.downloadLink").value(params.get("downloadLink")))
        .andExpect(jsonPath("$.size").value(params.get("size")))
        .andExpect(jsonPath("$.totalRecords").value(params.get("totalRecords")))
        .andExpect(jsonPath("$.numberDatasets").value(params.get("numberDatasets")));
    }
  }

  @Then("null sql download assertions passed")
  public void checkGetNullSqlDownloadResponse(DataTable dataTable) throws Exception {
    for (Map<String, String> params : dataTable.asMaps()) {
      result
        .andExpect(jsonPath("$.license").value(params.get("license")))
        .andExpect(jsonPath("$.key").value(download.getKey()))
        .andExpect(jsonPath("$.doi").value(isDownloadDoi()))
        .andExpect(jsonPath("$.request.sendNotification").value(params.get("sendNotification")))
        .andExpect(jsonPath("$.request.format").value(params.get("format")))
        .andExpect(jsonPath("$.created").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.modified").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.eraseAfter").value(isRegistryDateFormat()))
        .andExpect(jsonPath("$.status").value(params.get("status")))
        .andExpect(jsonPath("$.downloadLink").value(params.get("downloadLink")))
        .andExpect(jsonPath("$.size").value(params.get("size")))
        .andExpect(jsonPath("$.totalRecords").value(params.get("totalRecords")))
        .andExpect(jsonPath("$.numberDatasets").value(params.get("numberDatasets")));
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
    }
  }

  @Then("{int} downloads in occurrence downloads list response")
  public void checkOccurrenceDownloadList(int numberOrRecords) throws Exception {
    result.andExpect(jsonPath("$.count").value(6));
  }

  /**
   * Creates {@link Download} instance with test data using a predicate request.
   * The key is generated randomly using the class java.util.UUID.
   * The instance generated should be ready and valid to be persisted.
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
