package org.gbif.registry.ws.resources.organization;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.gbif.registry.ws.resources.SpringIT;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    strict = true,
    features = {
        "classpath:features/organization/organization_positive.feature"
    },
    glue = "org.gbif.registry.ws.resources.organization",
    plugin = "pretty"
)
public class OrganizationIT extends SpringIT {
}
