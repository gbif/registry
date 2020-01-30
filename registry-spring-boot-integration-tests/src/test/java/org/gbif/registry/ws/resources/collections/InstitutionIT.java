package org.gbif.registry.ws.resources.collections;

import org.gbif.registry.DatabaseInitializer;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/institution.feature"
  },
  glue = {
    "org.gbif.registry.ws.resources.collections.institution",
    "org.gbif.registry.utils.cucumber"
  },
  plugin = "pretty"
)
public class InstitutionIT {

  @ClassRule
  public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

}
