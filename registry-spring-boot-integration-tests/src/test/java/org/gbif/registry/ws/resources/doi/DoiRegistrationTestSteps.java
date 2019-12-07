package org.gbif.registry.ws.resources.doi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DoiRegistrationTestSteps {

  private ResultActions result;
  private MockMvc mvc;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper objectMapper;

  @Before("@DoiRegistration")
  public void setUp() throws Exception {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @When("generate new DOI of type {string}")
  public void changePassword(String doiType) throws Exception {
    result = mvc
      .perform(
        post("/doi/gen/{type}", doiType)
          .contentType(MediaType.APPLICATION_JSON)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("register DOI of type {string} for entity with key {string}")
  public void registerDoi(String doiType, String key) throws Exception {
    DoiRegistration data = DoiRegistration.builder()
      .withType(DoiType.valueOf(doiType))
      .withUser(TEST_ADMIN)
      .withMetadata(testMetadata())
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

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("DOI is returned")
  public void assertDoiWasReturned() throws Exception {
    String doi = RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
    assertFalse(doi.isEmpty());
  }

  /**
   * Create a test DataCiteMetadata instance.
   */
  public String testMetadata() {
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
