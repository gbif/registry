package org.gbif.registry.ws.resources.occurrencedownload;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.gbif.registry.utils.matcher.RegistryMatchers.isDownloadDoi;
import static org.gbif.registry.utils.matcher.RegistryMatchers.isRegistryDateFormat;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

  @Before("@OccurrenceDownload")
  public void setUp() throws Exception {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @Given("instance predicate download")
  public void prepareInstancePredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
      new PredicateDownloadRequest(new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "212"),
        TestConstants.TEST_ADMIN, Collections.singleton("downloadtest@gbif.org"),
        true, DownloadFormat.DWCA));
    this.download = download;
  }

  @When("create download")
  public void createDownload() throws Exception {
    String stringContent = objectMapper.writeValueAsString(download);

    result = mvc
      .perform(
        post("/occurrence/download")
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .contentType(MediaType.APPLICATION_JSON)
          .content(stringContent))
      .andDo(print());
  }

  @When("get download")
  public void getDownload() throws Exception {
    result = mvc
      .perform(
        get("/occurrence/download/{key}", download.getKey())
          .contentType(MediaType.APPLICATION_JSON))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("download assertions passed")
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

  /**
   * Creates {@link Download} instance with test data using a predicate request.
   * The key is generated randomly using the class java.util.UUID.
   * The instance generated should be ready and valid to be persisted.
   */
  protected static Download getTestInstanceDownload() {
    Download download = new Download();
    download.setKey(UUID.randomUUID().toString());
    download.setStatus(Download.Status.PREPARING);
    download.setDoi(new DOI("doi:10.1234/1ASCDU"));
    download.setDownloadLink("testUrl");
    download.setEraseAfter(Date.from(OffsetDateTime.now(ZoneOffset.UTC).plusMonths(6).toInstant()));

    return download;
  }

}
