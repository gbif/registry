package org.gbif.registry.ws.resources.legacy;

import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.utils.Requests;
import org.gbif.utils.HttpUtil;

import java.io.IOException;
import java.net.URISyntaxException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ThesaurusResourceIT {

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * The test sends a get all vocabularies (GET) request, the JSON response having a title, description,
   * subject, identifier, and url for each vocabulary in the list.
   */
  @Test
  public void testGetThesauri() throws IOException, URISyntaxException, SAXException {

    // construct request uri
    String uri = Requests.getRequestUri("/registry/thesauri.json");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    JsonNode rootNode = objectMapper.readTree(result.content);

    // JSON object expected, with single key/value array "thesauri":[{},..]
    assertTrue(result.content.startsWith("{\"thesauri\":["));
    assertEquals(1, rootNode.size());

    JsonNode thesauriNode = rootNode.get("thesauri");
    assertEquals(50, thesauriNode.size());
    assertNotNull(thesauriNode.get(0).get("title"));
    assertNotNull(thesauriNode.get(0).get("description"));
    assertNotNull(thesauriNode.get(0).get("subject"));
    assertNotNull(thesauriNode.get(0).get("identifier"));
    assertNotNull(thesauriNode.get(0).get("url"));
  }
}
