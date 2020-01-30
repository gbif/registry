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
    "classpath:features/user_management.feature"
  },
  glue = {"org.gbif.registry.ws.resources.usermanagement", "org.gbif.registry.utils.cucumber"},
  plugin = "pretty"
)
public class UserManagementIT {

  @ClassRule
  public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

}
