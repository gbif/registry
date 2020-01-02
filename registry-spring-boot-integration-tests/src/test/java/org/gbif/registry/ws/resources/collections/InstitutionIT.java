package org.gbif.registry.ws.resources.collections;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/institution.feature"
  },
  glue = "org.gbif.registry.ws.resources.collections.institution",
  plugin = "pretty"
)
public class InstitutionIT {
}
