package org.gbif.registry.ws.resources.networkentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.TestEmailConfiguration;
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
import static org.gbif.registry.ws.resources.networkentity.NetworkEntityProvider.ENTITIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
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
    expectedEntity = NetworkEntityProvider.prepare(entityType, UK_NODE_KEY, ORGANIZATION_KEY, INSTALLATION_KEY);
    expectedEntity.setTitle("New entity " + entityType);
    String entityJson = objectMapper.writeValueAsString(expectedEntity);

    result = mvc
      .perform(
        post("/" + entityType)
          .with(httpBasic("justadmin", "welcome"))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON))
      .andDo(print());

    key =
      RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
    expectedEntity.setKey(UUID.fromString(key));
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
          .with(httpBasic("justadmin", "welcome"))
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
          .with(httpBasic("justadmin", "welcome")));
  }

  @When("list {word} contacts")
  public void listEntityContacts(String entityType) {
    try {
      result = mvc
        .perform(
          get("/" + entityType + "/{key}/contact", key));
    } catch (Exception e) {
      // TODO: 25/10/2019 for 'node' case
    }
  }

  @SuppressWarnings("unchecked")
  @Then("{word} contacts list should be empty")
  public void checkContactsListEmpty(String entityType) throws Exception {
    if (entityType.equals("node")) {
      // TODO: 25/10/2019 node returns 201 instead of 200 for some reason
    } else {
      List<Contact> contacts = (List<Contact>) objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), List.class);

      assertNotNull("Contact list should be empty, not null when no contacts exist", contacts);
      assertTrue("Contact should be empty when none added", contacts.isEmpty());
    }
  }

  @When("add contact to {word}")
  public void addContactToEntity(String entityType) throws Exception {
    Contact contact = Contacts.newInstance();
    String entityJson = objectMapper.writeValueAsString(contact);

    result = mvc
      .perform(
        post("/" + entityType + "/{key}/contact", key)
          .with(httpBasic("justadmin", "welcome"))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }
}
