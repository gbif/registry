package org.gbif.registry.ws.resources.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Network;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Networks;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.resources.NetworkResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertNotNull;
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
public class NetworkTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private UUID networkKey;
  private Network network;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private NetworkResource networkResource;

  @Autowired
  private WebApplicationContext context;

  @Before("@Network")
  public void setUp() throws Exception {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@Network")
  public void tearDown() throws Exception {
  }

  @When("create new network {string}")
  public void createNetwork(String networkName) throws Exception {
    Network network = Networks.newInstance();
    network.setTitle(networkName);

    String jsonContent = objectMapper.writeValueAsString(network);

    result = mvc
      .perform(
        post("/network")
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(jsonContent)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @When("update network {string}")
  public void updateNetwork(String networkName) throws Exception {
    network = networkResource.get(networkKey);
    assertNotNull(network);

    String jsonContent = objectMapper.writeValueAsString(network);

    result = mvc
      .perform(
        put("/network/{key}", networkKey)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(jsonContent)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON))
      .andDo(print());
  }

  @When("delete network {string} by key")
  public void deleteNetwork(String networkName) throws Exception {
    result = mvc
      .perform(
        delete("/network/{key}", networkKey)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("get network by key")
  public void getNetworkById() throws Exception {
    result = mvc
      .perform(
        get("/network/{key}", networkKey));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("network key is present in response")
  public void extractKeyFromResponse() throws Exception {
    networkKey =
      UUID.fromString(RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString()));
  }
}
