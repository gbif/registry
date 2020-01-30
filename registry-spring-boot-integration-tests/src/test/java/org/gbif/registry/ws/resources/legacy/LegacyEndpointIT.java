package org.gbif.registry.ws.resources.legacy;

import org.gbif.registry.DatabaseInitializer;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/legacy_endpoint.feature"
  },
  glue = {
    "org.gbif.registry.ws.resources.legacy.endpoint",
    "org.gbif.registry.utils.cucumber"
  },
  plugin = "pretty"
)
public class LegacyEndpointIT {

  @ClassRule
  public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

}
