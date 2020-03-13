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
package org.gbif.registry.ws.resources.node;

import org.gbif.api.model.registry.Node;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.resources.NodeResource;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

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

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NodeTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private UUID nodeKey;
  private Node node;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private NodeResource nodeResource;

  @Autowired private WebApplicationContext context;

  @Before("@Node")
  public void setUp() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@Node")
  public void tearDown() throws Exception {}

  @When("create new node {string} by {word} {string} and password {string}")
  public void createNode(String nodeName, String userType, String username, String password)
      throws Exception {
    Node node = Nodes.newInstance();
    node.setTitle(nodeName);

    String jsonContent = objectMapper.writeValueAsString(node);

    result =
        mvc.perform(
            post("/node")
                .with(httpBasic(username, password))
                .content(jsonContent)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("create new node {string}")
  public void createNode(String nodeName) throws Exception {
    createNode(nodeName, "admin", TEST_ADMIN, TEST_PASSWORD);
  }

  @When("update node {string}")
  public void updateNode(String nodeName) throws Exception {
    node = nodeResource.get(nodeKey);
    assertNotNull(node);

    String jsonContent = objectMapper.writeValueAsString(node);

    result =
        mvc.perform(
                put("/node/{key}", nodeKey)
                    .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                    .content(jsonContent)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
  }

  @When("delete node {string} by key")
  public void deleteNode(String nodeName) throws Exception {
    result = mvc.perform(delete("/node/{key}", nodeKey).with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("get node by key")
  public void getNodeById() throws Exception {
    result = mvc.perform(get("/node/{key}", nodeKey));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("node key is present in response")
  public void extractKeyFromResponse() throws Exception {
    nodeKey =
        UUID.fromString(
            RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString()));
  }
}
