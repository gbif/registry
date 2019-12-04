package org.gbif.registry.ws.resources.networkentity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Comments;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.Endpoints;
import org.gbif.registry.utils.MachineTags;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.utils.Tags;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.gbif.registry.utils.LenientAssert.assertLenientEquals;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.gbif.registry.ws.resources.networkentity.NetworkEntityProvider.ENTITIES;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NetworkEntityTestSteps {

  private static final UUID UK_NODE_KEY = UUID.fromString("f698c938-d36a-41ac-8120-c35903e1acb9");
  private static final UUID UK_NODE_2_KEY = UUID.fromString("9996f2f2-f71c-4f40-8e69-031917b314e0");
  private static final UUID ORGANIZATION_KEY = UUID.fromString("f433944a-ad93-4ea8-bad7-68de7348e65a");
  private static final UUID INSTALLATION_KEY = UUID.fromString("70d1ffaf-8e8a-4f40-9c5d-00a0ddbefa4c");

  private static final Map<String, UUID> NODE_MAP = new HashMap<>();

  private MockMvc mvc;
  private ResultActions result;
  private NetworkEntity actualEntity;
  private NetworkEntity expectedEntity;
  private String key;
  private Date modificationDateBeforeUpdate;
  private Date creationDateBeforeUpdate;
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

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Before("@NetworkEntity")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_node_prepare.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_prepare.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/network_installation_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();

    NODE_MAP.put("UK Node", UK_NODE_KEY);
    NODE_MAP.put("UK Node 2", UK_NODE_2_KEY);
  }

  @After("@NetworkEntity")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_cleanup.sql"));
    connection.close();
  }

  @When("create new {word}")
  public void createEntity(String entityType) throws Exception {
    createEntity(entityType, "admin", TEST_ADMIN, TEST_PASSWORD);
  }

  @When("create new {word} by {word} {string} and password {string}")
  public void createEntity(String entityType, String userType, String username, String password) throws Exception {
    expectedEntity = NetworkEntityProvider.prepare(entityType, UK_NODE_KEY, ORGANIZATION_KEY, INSTALLATION_KEY);
    expectedEntity.setTitle("New entity " + entityType);
    String entityJson = objectMapper.writeValueAsString(expectedEntity);

    result = mvc
      .perform(
        post("/" + entityType)
          .with(httpBasic(username, password))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @When("get {word} by key")
  public void getEntityById(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}", key));

    actualEntity = objectMapper.readValue(result.andReturn().getResponse().getContentAsByteArray(), ENTITIES.get(entityType));
  }

  @When("update {word} with new title {string}")
  public void updateEntity(String entityType, String newTitle) throws Exception {
    modificationDateBeforeUpdate = actualEntity.getModified();
    creationDateBeforeUpdate = actualEntity.getCreated();
    actualEntity.setTitle(newTitle);
    expectedEntity.setTitle(newTitle);

    String entityJson = objectMapper.writeValueAsString(actualEntity);

    result = mvc
      .perform(
        put("/" + entityType + "/{key}", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("title is new {string}")
  public void checkUpdatedEntityTitle(String newTitle) {
    assertEquals("Entity's title was to be updated", newTitle, actualEntity.getTitle());
  }

  @Then("modification date was updated")
  public void checkModificationDateWasChangedAfterUpdate() {
    assertNotNull(actualEntity.getModified());
    assertTrue("Modification date was to be changed",
      actualEntity.getModified().after(modificationDateBeforeUpdate));
  }

  @Then("modification date is after the creation date")
  public void checkModificationDateIsAfterCreationDate() {
    assertNotNull(actualEntity.getModified());
    assertTrue("Modification date must be after the creation date",
      actualEntity.getModified().after(creationDateBeforeUpdate));
  }

  @Then("modification and creation dates are present")
  public void checkModificationCreationDatesPresent() {
    assertNotNull(actualEntity.getCreated());
    assertNotNull(actualEntity.getModified());
    assertNotNull(actualEntity.getCreatedBy());
    assertNotNull(actualEntity.getModifiedBy());
  }

  @Then("{word} is not marked as deleted")
  public void checkEntityIsNotDeleted(String entityType) {
    assertNull(actualEntity.getDeleted());
  }

  @SuppressWarnings("unchecked")
  @Then("created {word} reflects the original one")
  public void checkCreatedEntityEqualsOriginalOne(String entityType) {
    // put processed properties (citation)
    if (entityType.equals("dataset")) {
      ((Dataset) expectedEntity).setCitation(((Dataset) actualEntity).getCitation());
    }
    assertLenientEquals("Persisted does not reflect original", ((LenientEquals) actualEntity), expectedEntity);
  }

  @SuppressWarnings("unchecked")
  @Then("deleted {word} reflects the original one")
  public void checkDeletedEntityEqualsOriginalOne(String entityType) {
    // put processed properties (citation)
    if (entityType.equals("dataset")) {
      ((Dataset) expectedEntity).setCitation(((Dataset) actualEntity).getCitation());
    }
    expectedEntity.setDeleted(actualEntity.getDeleted());
    assertLenientEquals("Persisted does not reflect original", ((LenientEquals) actualEntity), expectedEntity);
  }

  @When("delete {word} by key")
  public void deleteEntityByKey(String entityType) throws Exception {
    result = mvc
      .perform(
        delete("/" + entityType + "/{key}", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("list {word} contacts")
  public void listEntityContacts(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}/contact", key));
  }

  @Then("{word} contacts list should contain {int} contacts")
  public void checkContactsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    contacts = objectMapper.readValue(jsonString, new TypeReference<List<Contact>>() {
    });
    assertNotNull(contacts);
    assertThat(contacts, hasSize(quantity));
  }

  @Then("only second contact is primary")
  public void checkSecondContactIsPrimary() {
    assertFalse("Older contact (added first) should not be primary anymore", contacts.get(0).isPrimary());
    assertTrue("Newer contact (added second) should now be primary", contacts.get(1).isPrimary());
  }

  @When("add {word} contact to {word}")
  public void addContactToEntity(String number, String entityType) throws Exception {
    expectedContact = Contacts.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedContact);

    result = mvc
      .perform(
        post("/" + entityType + "/{key}/contact", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  // TODO: 26/10/2019 add update contact case
  @When("delete {word} contact")
  public void deleteContact(String entityType) throws Exception {
    result = mvc
      .perform(
        delete("/" + entityType + "/{key}/contact/{contactKey}", key, contacts.get(0).getKey())
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @Then("{word} contact reflects the original one")
  public void checkEntityContact(String entityType) {
    assertLenientEquals("Created contact does not read as expected", expectedContact, contacts.get(0));
  }

  @When("list {word} endpoints")
  public void listEntityEndpoints(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}/endpoint", key));
  }

  @Then("{word} endpoints list should contain {int} endpoints")
  public void checkEndpointsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    endpoints = objectMapper.readValue(jsonString, new TypeReference<List<Endpoint>>() {
    });
    assertNotNull(endpoints);
    assertThat(endpoints, hasSize(quantity));
  }

  @When("add {word} endpoint to {word}")
  public void addEndpointToEntity(String number, String entityType) throws Exception {
    expectedEndpoint = Endpoints.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedEndpoint);

    result = mvc
      .perform(
        post("/" + entityType + "/{key}/endpoint", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @When("list {word} comments")
  public void listEntityComments(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}/comment", key));
  }

  @Then("{word} comments list should contain {int} comments")
  public void checkCommentsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    comments = objectMapper.readValue(jsonString, new TypeReference<List<Comment>>() {
    });
    assertNotNull(comments);
    assertThat(comments, hasSize(quantity));
  }

  @When("add {word} comment to {word}")
  public void addCommentToEntity(String number, String entityType) throws Exception {
    expectedComment = Comments.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedComment);

    result = mvc
      .perform(
        post("/" + entityType + "/{key}/comment", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @When("list {word} machine tags")
  public void listEntityMachineTags(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}/machineTag", key));
  }

  @Then("{word} machine tags list should contain {int} machine tags")
  public void checkMachineTagsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    machineTags = objectMapper.readValue(jsonString, new TypeReference<List<MachineTag>>() {
    });
    assertNotNull(machineTags);
    assertThat(machineTags, hasSize(quantity));
  }

  @When("add {word} machine tag to {word}")
  public void addMachineTagToEntity(String number, String entityType) throws Exception {
    expectedMachineTag = MachineTags.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedMachineTag);

    result = mvc
      .perform(
        post("/" + entityType + "/{key}/machineTag", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @When("list {word} tags")
  public void listEntityTags(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}/tag", key));
  }

  @Then("{word} tags list should contain {int} tags")
  public void checkTagsList(String entityType, int quantity) throws Exception {
    String jsonString = result.andReturn().getResponse().getContentAsString();
    tags = objectMapper.readValue(jsonString, new TypeReference<List<Tag>>() {
    });
    assertNotNull(tags);
    assertThat(tags, hasSize(quantity));
  }

  @When("add {word} tag to {word}")
  public void addTagToEntity(String number, String entityType) throws Exception {
    expectedTag = Tags.newInstance();
    String entityJson = objectMapper.writeValueAsString(expectedTag);

    result = mvc
      .perform(
        post("/" + entityType + "/{key}/tag", key)
          .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("{word} key is present in response")
  public void extractKeyFromResponse(String entityType) throws Exception {
    key = RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
    expectedEntity.setKey(UUID.fromString(key));
  }
}
