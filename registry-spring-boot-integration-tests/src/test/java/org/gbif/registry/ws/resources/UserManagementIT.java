package org.gbif.registry.ws.resources;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/user_management.feature"
  },
  glue = "org.gbif.registry.ws.resources.usermanagement",
  plugin = "pretty"
)
public class UserManagementIT {
}
