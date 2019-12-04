package org.gbif.registry.ws.resources;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/node.feature"
  },
  glue = "org.gbif.registry.ws.resources.node",
  plugin = "pretty"
)
public class NodeIT {
}
