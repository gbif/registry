package org.gbif.registry.ws.resources;

import org.gbif.registry.DatabaseInitializer;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/doi_registration.feature"
  },
  glue = "org.gbif.registry.ws.resources.doi",
  plugin = "pretty"
)
public class DoiRegistrationIT {

  @ClassRule
  public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

}
