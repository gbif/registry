package org.gbif.registry;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.gbif.ws.server.interceptor",
    "org.gbif.ws.server.filter", "org.gbif.ws.security", "org.gbif.query", "org.gbif.registry"})
@PropertySource("classpath:application-test.yml")
public class RegistryIntegrationTestsConfiguration {
}
