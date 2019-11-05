package org.gbif.registry.ws.resources.legacy;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/ipt.feature"
  },
  glue = "org.gbif.registry.ws.resources.legacy.ipt",
  plugin = "pretty"
  ,tags = "@UpdateIpt"
)
public class IptIT {
}
