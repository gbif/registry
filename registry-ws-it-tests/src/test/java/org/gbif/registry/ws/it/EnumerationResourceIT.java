/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it;

import org.gbif.registry.ws.resources.EnumerationResource;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test to make sure we can produce the Enumeration response. We use a simple Jersey Client
 * since it's not available in the Java client.
 */
@SpringBootTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false",
      "spring.flyway.enabled=false"
    })
@Import(EnumerationResource.class)
@AutoConfigureMockMvc(addFilters = false)
public class EnumerationResourceIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  public void testTermEnumeration() throws Exception {
    MvcResult result =
        mockMvc.perform(MockMvcRequestBuilders.get("/enumeration/basic")).andReturn();
    List<String> responseContent =
        objectMapper.readValue(
            result.getResponse().getContentAsByteArray(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    assertNotNull(responseContent);
    assertTrue(responseContent.size() > 0);
  }

  @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
  @ComponentScan(
      basePackages = "org.gbif.registry.ws.resources",
      resourcePattern = "**/EnumerationResource.class")
  public static class TestItApp {
    public static void main(String[] args) throws Exception {
      SpringApplication.run(TestItApp.class, args);
    }
  }
}
