package org.gbif.registry.ws.resources.doi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.ObjectFactory;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.utils.RegistryITUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DoiRegistrationTestSteps {

  private ResultActions result;
  private MockMvc mvc;
  private DOI doi;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Before("@DoiRegistration")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/doi/doi_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/doi/doi_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@DoiRegistration")
  public void cleanAfterUserCrud() {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/doi/doi_cleanup.sql"));
  }

  @Given("existing DOI {string} with status {string}")
  public void prepareDoi(String doi, String doiStatus) {
    // prepared by scripts in @Before
  }

  @When("generate new DOI of type {string}")
  public void changePassword(String doiType) throws Exception {
    result = mvc
      .perform(
        post("/doi/gen/{type}", doiType)
          .contentType(MediaType.APPLICATION_JSON)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("register DOI {string} of type {string} for entity with key {string} and metadata parameters")
  public void registerDoi(String doiStr, String doiType, String key, Map<String, String> params) throws Exception {
    DoiRegistration data = DoiRegistration.builder()
      .withType(DoiType.valueOf(doiType))
      .withUser(TEST_ADMIN)
      .withMetadata(testMetadata(params))
      .withDoi(new DOI(doiStr))
      .build();

    if (!"DATA_PACKAGE".equals(doiType)) {
      data.setKey(key);
    }

    String jsonContent = objectMapper.writeValueAsString(data);

    result = mvc
      .perform(
        post("/doi", doiType)
          .content(jsonContent)
          .contentType(MediaType.APPLICATION_JSON)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("register DOI of type {string} for entity with key {string} and metadata parameters")
  public void registerDoi(String doiType, String key, Map<String, String> params) throws Exception {
    registerDoi(null, doiType, key, params);
  }

  @When("update DOI {string} of type {string} for entity with key {string} and metadata parameters")
  public void updateDoi(String doiStr, String doiType, String key, Map<String, String> params) throws Exception {
    DoiRegistration data = DoiRegistration.builder()
      .withType(DoiType.valueOf(doiType))
      .withUser(TEST_ADMIN)
      .withMetadata(testMetadata(params))
      .withDoi(new DOI(doiStr))
      .build();

    if (!"DATA_PACKAGE".equals(doiType)) {
      data.setKey(key);
    }

    String jsonContent = objectMapper.writeValueAsString(data);

    result = mvc
      .perform(
        put("/doi", doiType)
          .content(jsonContent)
          .contentType(MediaType.APPLICATION_JSON)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("update DOI of type {string} for entity with key {string} and metadata parameters")
  public void updateDoi(String doiType, String key, Map<String, String> params) throws Exception {
    updateDoi(null, doiType, key, params);
  }

  @When("delete DOI of type {string}")
  public void deleteDoi(String doiType) throws Exception {
    result = mvc
      .perform(
        delete("/doi/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix())
      );
  }

  @When("get DOI")
  public void getDoi() throws Exception {
    result = mvc
      .perform(
        get("/doi/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix())
          .contentType(MediaType.APPLICATION_JSON))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("DOI is returned")
  public void assertDoiWasReturned() throws Exception {
    String doiStr = RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
    doi = new DOI(doiStr);
    assertFalse(doiStr.isEmpty());
  }

  /**
   * Create a test DataCiteMetadata instance.
   */
  private String testMetadata(Map<String, String> params) {
    try {
      ObjectFactory of = new ObjectFactory();
      DataCiteMetadata res = of.createDataCiteMetadata();

      DataCiteMetadata.Creators creators = of.createDataCiteMetadataCreators();
      DataCiteMetadata.Creators.Creator creator = of.createDataCiteMetadataCreatorsCreator();
      DataCiteMetadata.Creators.Creator.CreatorName name = of.createDataCiteMetadataCreatorsCreatorCreatorName();
      name.setValue(TEST_ADMIN);
      creator.setCreatorName(name);
      creators.getCreator().add(creator);
      res.setCreators(creators);

      DataCiteMetadata.Titles titles = of.createDataCiteMetadataTitles();
      DataCiteMetadata.Titles.Title title = of.createDataCiteMetadataTitlesTitle();
      title.setValue("TEST Tile");
      title.setValue(params.get("title"));
      titles.getTitle().add(title);
      res.setTitles(titles);

      res.setPublicationYear("2017");
      DataCiteMetadata.Publisher publisher = of.createDataCiteMetadataPublisher();
      publisher.setValue(TEST_ADMIN);
      res.setPublisher(publisher);
      DataCiteMetadata.ResourceType resourceType = of.createDataCiteMetadataResourceType();
      resourceType.setResourceTypeGeneral(ResourceType.DATASET);
      res.setResourceType(resourceType);
      return DataCiteValidator.toXml(new DOI(DOI.TEST_PREFIX, "1"), res);
    } catch (InvalidMetadataException ex) {
      throw new RuntimeException(ex);
    }
  }
}
