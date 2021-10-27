/*
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
package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.DOI;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivedDatasetCreationRequestTest {

  private final ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapper();

  @Test
  public void testInvalidDoiDeser() {
    // given
    String json =
        "{\n"
            + "  \"originalDownloadDOI\": \"11.21373/dl.njvgg8\",\n"
            + "  \"sourceUrl\": \"https://github.com/gbif/registry\",\n"
            + "  \"title\": \"Derived dataset\",\n"
            + "  \"description\": \"Derived dataset description\",\n"
            + "  \"registrationDate\": \"2020-09-22T14:09:22.000+0000\",\n"
            + "  \"relatedDatasets\": {\n"
            + "    \"10.21373/1llmgl\": 1,\n"
            + "    \"9579eaa0-f762-11e1-a439-00145eb45e9a\": 2,\n"
            + "    \"10.21373/abcdef\": 3\n"
            + "  }\n"
            + "}";

    // when & then
    JsonMappingException exception =
        assertThrows(
            JsonMappingException.class,
            () -> objectMapper.readValue(json, DerivedDatasetCreationRequest.class));

    assertTrue(exception.getMessage().contains("is not a valid DOI"));
  }

  @Test
  public void testInvalidRegistrationDateDeser() {
    // given
    String json =
        "{\n"
            + "  \"originalDownloadDOI\": \"10.21373/dl.njvgg8\",\n"
            + "  \"sourceUrl\": \"https://github.com/gbif/registry\",\n"
            + "  \"title\": \"Derived dataset\",\n"
            + "  \"description\": \"Derived dataset description\",\n"
            + "  \"registrationDate\": \"2020-0129\",\n"
            + "  \"relatedDatasets\": {\n"
            + "    \"10.21373/1llmgl\": 1,\n"
            + "    \"9579eaa0-f762-11e1-a439-00145eb45e9a\": 2,\n"
            + "    \"10.21373/abcdef\": 3\n"
            + "  }\n"
            + "}";

    // when & then
    JsonMappingException exception =
        assertThrows(
            JsonMappingException.class,
            () -> objectMapper.readValue(json, DerivedDatasetCreationRequest.class));
    assertTrue(exception.getMessage().contains("java.util.Date"));
    assertTrue(exception.getMessage().contains("Cannot parse date"));
  }

  @Test
  public void testRegisteredDatasetsDuplicatesDeser() {
    // given
    String json =
        "{\n"
            + "  \"originalDownloadDOI\": \"10.21373/dl.njvgg8\",\n"
            + "  \"sourceUrl\": \"https://github.com/gbif/registry\",\n"
            + "  \"title\": \"Derived dataset\",\n"
            + "  \"description\": \"Derived dataset description\",\n"
            + "  \"registrationDate\": \"2020-01-29\",\n"
            + "  \"relatedDatasets\": {\n"
            + "    \"9579eaa0-f762-11e1-a439-00145eb45e9a\": 1,\n"
            + "    \"9579eaa0-f762-11e1-a439-00145eb45e9a\": 2\n"
            + "  }\n"
            + "}";

    // when & then
    JsonMappingException exception =
        assertThrows(
            JsonMappingException.class,
            () -> objectMapper.readValue(json, DerivedDatasetCreationRequest.class));
    assertTrue(exception.getMessage().contains("Duplicate field"));
  }

  @Test
  public void testInvalidSourceUrlDeser() {
    // given
    String json =
        "{\n"
            + "  \"originalDownloadDOI\": \"10.21373/dl.njvgg8\",\n"
            + "  \"sourceUrl\": \"://github.com/gbif/registry\",\n"
            + "  \"title\": \"Derived dataset\",\n"
            + "  \"description\": \"Derived dataset description\",\n"
            + "  \"registrationDate\": \"2020-0129\",\n"
            + "  \"relatedDatasets\": {\n"
            + "    \"10.21373/1llmgl\": 1,\n"
            + "    \"9579eaa0-f762-11e1-a439-00145eb45e9a\": 2,\n"
            + "    \"10.21373/abcdef\": 3\n"
            + "  }\n"
            + "}";

    // when & then
    JsonMappingException exception =
        assertThrows(
            JsonMappingException.class,
            () -> objectMapper.readValue(json, DerivedDatasetCreationRequest.class));
    System.out.println(exception.getMessage());
    assertTrue(exception.getMessage().contains("Expected scheme"));
  }

  @Test
  public void testNullsDeser() throws Exception {
    // given
    // use nulls explicitly to be sure they'll be deserialized properly
    String json =
        "{\n"
            + "  \"originalDownloadDOI\": null,\n"
            + "  \"sourceUrl\": null,\n"
            + "  \"registrationDate\": null,\n"
            + "  \"description\": null,\n"
            + "  \"title\": null,\n"
            + "  \"relatedDatasets\": null"
            + "}";
    DerivedDatasetCreationRequest expected = new DerivedDatasetCreationRequest();

    // when
    DerivedDatasetCreationRequest actual =
        objectMapper.readValue(json, DerivedDatasetCreationRequest.class);

    // then
    assertEquals(expected, actual);
  }

  @Test
  public void testDefaultSerde() throws Exception {
    // given
    DerivedDatasetCreationRequest source = new DerivedDatasetCreationRequest();

    // when
    String json = objectMapper.writeValueAsString(source);
    DerivedDatasetCreationRequest actual =
        objectMapper.readValue(json, DerivedDatasetCreationRequest.class);

    // then
    assertEquals(source, actual);
  }

  @Test
  public void testSuccessfulSerde() throws Exception {
    // given
    DerivedDatasetCreationRequest source = prepareInstance();

    // when
    String json = objectMapper.writeValueAsString(source);
    DerivedDatasetCreationRequest actual =
        objectMapper.readValue(json, DerivedDatasetCreationRequest.class);

    // then
    assertEquals(source, actual);
  }

  private DerivedDatasetCreationRequest prepareInstance() {
    DerivedDatasetCreationRequest instance = new DerivedDatasetCreationRequest();
    instance.setOriginalDownloadDOI(new DOI("10.21373/dl.njvgg8"));
    instance.setRegistrationDate(new Date());
    instance.setTitle("Derived dataset");
    instance.setDescription("description");
    instance.setSourceUrl(URI.create("http://github.com/gbif/registry"));

    Map<String, Long> relatedDatasets = new HashMap<>();
    relatedDatasets.put("10.21373/1llmgl", 1L);
    relatedDatasets.put("9579eaa0-f762-11e1-a439-00145eb45e9a", 2L);
    relatedDatasets.put("10.21373/abcdef", 3L);
    instance.setRelatedDatasets(relatedDatasets);

    return instance;
  }
}
