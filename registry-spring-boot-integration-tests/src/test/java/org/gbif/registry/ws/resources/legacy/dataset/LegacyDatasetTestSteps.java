package org.gbif.registry.ws.resources.legacy.dataset;

import io.cucumber.java.en.Given;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestEmailConfiguration.class,
  RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LegacyDatasetTestSteps {

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    // prepared by script, see @Before
  }

  @Given("installation {string} with key {string}")
  public void prepareInstallation(String instName, String installationKey) {
    // prepared by script, see @Before
  }

  @Given("dataset {string} with key {string}")
  public void prepareDataset(String name, String datasetKey) {
    // prepared by script, see @Before
  }

  @Given("dataset contact {string} with key {int}")
  public void prepareContact(String email, int key) {
    // prepared by script, see @Before
  }

  @Given("dataset endpoint {string} with key {int}")
  public void prepareEndpoint(String description, int key) {
    // prepared by script, see @Before
  }
}
