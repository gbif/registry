package org.gbif.registry.ws.resources;

import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SpringIT {
}
