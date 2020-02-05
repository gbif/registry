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
package org.gbif.registry.ws.resources.networkentity;

import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Comments;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.Endpoints;
import org.gbif.registry.utils.MachineTags;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.utils.Tags;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.utils.LenientAssert.assertLenientEquals;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NetworkEntityTestSteps {

  private static final UUID NODE_KEY = UUID.fromString("f698c938-d36a-41ac-8120-c35903e1acb9");
  private static final UUID NETWORK_KEY = UUID.fromString("83a2c5d5-5d85-4c9e-ac80-dc9e14766e7e");
  private static final UUID ORGANIZATION_KEY =
      UUID.fromString("f433944a-ad93-4ea8-bad7-68de7348e65a");
  private static final UUID INSTALLATION_KEY =
      UUID.fromString("70d1ffaf-8e8a-4f40-9c5d-00a0ddbefa4c");
  private static final UUID DATASET_KEY = UUID.fromString("d82273f6-9738-48a5-a639-2086f9c49d18");

  private static final Map<String, NetworkEntityService> SERVICES = new HashMap<>();
  private static final Map<String, UUID> ENTITY_KEYS = new HashMap<>();

  static {
    ENTITY_KEYS.put("node", NODE_KEY);
    ENTITY_KEYS.put("network", NETWORK_KEY);
    ENTITY_KEYS.put("organization", ORGANIZATION_KEY);
    ENTITY_KEYS.put("installation", INSTALLATION_KEY);
    ENTITY_KEYS.put("dataset", DATASET_KEY);
  }

  private MockMvc mvc;
  private ResultActions result;
  private UUID entityKey;
  private List<Contact> contacts;
  private Contact expectedContact;
  private List<Endpoint> endpoints;
  private Endpoint expectedEndpoint;
  private List<Comment> comments;
  private Comment expectedComment;
  private List<MachineTag> machineTags;
  private MachineTag expectedMachineTag;
  private List<Tag> tags;
  private Tag expectedTag;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private WebApplicationContext context;

  @Autowired private DataSource ds;

  private Connection connection;

  @Autowired private NodeService nodeService;

  @Autowired private NetworkService networkService;

  @Autowired private OrganizationService organizationService;

  @Autowired private InstallationService installationService;

  @Autowired private DatasetService datasetService;

  @Before("@NetworkEntity")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/network_entities_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/networkentity/network_entity_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    SERVICES.put("node", nodeService);
    SERVICES.put("network", networkService);
    SERVICES.put("organization", organizationService);
    SERVICES.put("installation", installationService);
    SERVICES.put("dataset", datasetService);
  }

  @After("@NetworkEntity")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/network_entities_cleanup.sql"));
    connection.close();
  }

  @When("create new {word}")
  public void createEntity(String entityType) throws Exception {
    createEntity(entityType, "admin", TEST_ADMIN, TEST_PASSWORD);
  }

  @When("create new {word} by {word} {string} and password {string}")
  public void createEntity(String entityType, String userType, String username, String password)
      throws Exception {
    NetworkEntity entity =
        NetworkEntityProvider.prepare(entityType, NODE_KEY, ORGANIZATION_KEY, INSTALLATION_KEY);
    entity.setTitle("New entity " + entityType);
    String entityJson = objectMapper.writeValueAsString(entity);

    result =
        mvc.perform(
            post("/" + entityType)
                .with(httpBasic(username, password))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @When("update {word} by {word} {string} and password {string}")
  public void updateEntity(
      String entityType,
      String userType,
      String username,
      String password,
      Map<String, String> fieldsToUpdate)
      throws Exception {
    NetworkEntityService service = SERVICES.get(entityType);
    UUID key = ENTITY_KEYS.get(entityType);
    NetworkEntity entity = ((NetworkEntity) service.get(key));

    for (Map.Entry<String, String> entry : fieldsToUpdate.entrySet()) {
      BeanUtils.setProperty(entity, entry.getKey(), entry.getValue());
    }

    String entityJson = objectMapper.writeValueAsString(entity);

    result =
        mvc.perform(
            put("/" + entityType + "/{key}", key)
                .with(httpBasic(username, password))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("update {word}")
  public void updateEntity(String entityType, Map<String, String> fieldsToUpdate) throws Exception {
    updateEntity(entityType, "admin", TEST_ADMIN, TEST_PASSWORD, fieldsToUpdate);
  }

  @When("delete {word} by key")
  public void deleteEntityByKey(String entityType) throws Exception {
    result =
        mvc.perform(
            delete("/" + entityType + "/{key}", entityKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("list {word} contacts")
  public void listEntityContacts(String entityType) throws Exception {
    result = mvc.perform(get("/" + entityType + "/{key}/contact", entityKey));
  }

  @Then("{word} contacts list should contain {int} contacts")
  public void checkContactsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    contacts = objectMapper.readValue(jsonString, new TypeReference<List<Contact>>() {});
    assertNotNull(contacts);
    assertThat(contacts, hasSize(quantity));
  }

  @Then("only second contact is primary")
  public void checkSecondContactIsPrimary() {
    assertFalse(
        "Older contact (added first) should not be primary anymore", contacts.get(0).isPrimary());
    assertTrue("Newer contact (added second) should now be primary", contacts.get(1).isPrimary());
  }

  @When("add {word} contact to {word}")
  public void addContactToEntity(String number, String entityType) throws Exception {
    expectedContact = Contacts.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedContact);

    result =
        mvc.perform(
            post("/" + entityType + "/{key}/contact", entityKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("delete {word} contact")
  public void deleteContact(String entityType) throws Exception {
    result =
        mvc.perform(
            delete(
                    "/" + entityType + "/{key}/contact/{contactKey}",
                    entityKey,
                    contacts.get(0).getKey())
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @Then("{word} contact reflects the original one")
  public void checkEntityContact(String entityType) {
    assertLenientEquals(
        "Created contact does not read as expected", expectedContact, contacts.get(0));
  }

  @When("list {word} endpoints")
  public void listEntityEndpoints(String entityType) throws Exception {
    result = mvc.perform(get("/" + entityType + "/{key}/endpoint", entityKey));
  }

  @Then("{word} endpoints list should contain {int} endpoints")
  public void checkEndpointsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    endpoints = objectMapper.readValue(jsonString, new TypeReference<List<Endpoint>>() {});
    assertNotNull(endpoints);
    assertThat(endpoints, hasSize(quantity));
  }

  @When("add {word} endpoint to {word}")
  public void addEndpointToEntity(String number, String entityType) throws Exception {
    expectedEndpoint = Endpoints.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedEndpoint);

    result =
        mvc.perform(
            post("/" + entityType + "/{key}/endpoint", entityKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("list {word} comments")
  public void listEntityComments(String entityType) throws Exception {
    result = mvc.perform(get("/" + entityType + "/{key}/comment", entityKey));
  }

  @Then("{word} comments list should contain {int} comments")
  public void checkCommentsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    comments = objectMapper.readValue(jsonString, new TypeReference<List<Comment>>() {});
    assertNotNull(comments);
    assertThat(comments, hasSize(quantity));
  }

  @When("add {word} comment to {word}")
  public void addCommentToEntity(String number, String entityType) throws Exception {
    expectedComment = Comments.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedComment);

    result =
        mvc.perform(
            post("/" + entityType + "/{key}/comment", entityKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("list {word} machine tags")
  public void listEntityMachineTags(String entityType) throws Exception {
    result = mvc.perform(get("/" + entityType + "/{key}/machineTag", entityKey));
  }

  @Then("{word} machine tags list should contain {int} machine tags")
  public void checkMachineTagsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    machineTags = objectMapper.readValue(jsonString, new TypeReference<List<MachineTag>>() {});
    assertNotNull(machineTags);
    assertThat(machineTags, hasSize(quantity));
  }

  @When("add {word} machine tag to {word}")
  public void addMachineTagToEntity(String number, String entityType) throws Exception {
    expectedMachineTag = MachineTags.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedMachineTag);

    result =
        mvc.perform(
            post("/" + entityType + "/{key}/machineTag", entityKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("list {word} tags")
  public void listEntityTags(String entityType) throws Exception {
    result = mvc.perform(get("/" + entityType + "/{key}/tag", entityKey));
  }

  @Then("{word} tags list should contain {int} tags")
  public void checkTagsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    tags = objectMapper.readValue(jsonString, new TypeReference<List<Tag>>() {});
    assertNotNull(tags);
    assertThat(tags, hasSize(quantity));
  }

  @When("add {word} tag to {word}")
  public void addTagToEntity(String number, String entityType) throws Exception {
    expectedTag = Tags.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedTag);

    result =
        mvc.perform(
            post("/" + entityType + "/{key}/tag", entityKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(entityJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("{word} key is present in response")
  public void extractKeyFromResponse(String entityType) throws Exception {
    entityKey =
        UUID.fromString(
            RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString()));
  }
}
