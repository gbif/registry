package org.gbif.registry.ws.resources.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Node;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.resources.NodeResource;
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
public class NodeTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private UUID nodeKey;
  private Node node;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private NodeResource nodeResource;

  @Autowired
  private WebApplicationContext context;

  @Before("@Node")
  public void setUp() throws Exception {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@Node")
  public void tearDown() throws Exception {
  }

  @When("create new node {string}")
  public void createNode(String nodeName) throws Exception {
    Node node = Nodes.newInstance();
    node.setTitle(nodeName);

    String jsonContent = objectMapper.writeValueAsString(node);

    result = mvc
      .perform(
        post("/node")
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(jsonContent)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @When("update node {string}")
  public void updateNode(String nodeName) throws Exception {
    node = nodeResource.get(nodeKey);
    assertNotNull(node);

    String jsonContent = objectMapper.writeValueAsString(node);

    result = mvc
      .perform(
        put("/node/{key}", nodeKey)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(jsonContent)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON))
      .andDo(print());
  }

  @When("delete node {string} by key")
  public void deleteNode(String nodeName) throws Exception {
    result = mvc
      .perform(
        delete("/node/{key}", nodeKey)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("get node by key")
  public void getNodeById() throws Exception {
    result = mvc
      .perform(
        get("/node/{key}", nodeKey));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("node key is present in response")
  public void extractKeyFromResponse() throws Exception {
    nodeKey =
      UUID.fromString(RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString()));
  }
}
