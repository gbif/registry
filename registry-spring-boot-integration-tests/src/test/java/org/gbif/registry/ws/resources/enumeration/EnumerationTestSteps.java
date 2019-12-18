package org.gbif.registry.ws.resources.enumeration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EnumerationTestSteps {

  private MockMvc mvc;
  private ResultActions result;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Before("@Enumeration")
  public void setUp() throws Exception {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @When("get enumeration inventory")
  public void getInventory() throws Exception {
    result = mvc
      .perform(
        get("/enumeration/basic"));
  }

  @When("get enumeration basic {string}")
  public void getEnum(String enumName) throws Exception {
    result = mvc
      .perform(
        get("/enumeration/basic/{name}", enumName));
  }

  @When("get {word} enumeration")
  public void getCountryEnum(String enumType) throws Exception {
    result = mvc
      .perform(
        get("/enumeration/" + enumType));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("element number {int} is {string}")
  public void assertOneElementOfArrayEnumResponse(int elementNumber, String elementValue) throws Exception {
    result.andExpect(jsonPath(String.format("$.[%d]", elementNumber)).value(elementValue));
  }

  @Then("element number {int} is")
  public void assertOneElementOfArrayEnumResponse(int elementNumber, Map<String, String> params) throws Exception {
    for (Map.Entry<String, String> entry : params.entrySet()) {
      result.andExpect(jsonPath(String.format("$[%d].%s", elementNumber, entry.getKey())).value(entry.getValue()));
    }
  }
}
