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

public class ExtensionResourceIT {

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * The test sends a get all extensions (GET) request, the JSON response having a title, description,
   * subject, identifier, and url for each extension in the list.
   */
  @Test
  public void testGetExtensions() throws IOException, URISyntaxException, SAXException {

    // construct request uri
    String uri = Requests.getRequestUri("/registry/extensions.json");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    JsonNode rootNode = objectMapper.readTree(result.content);

    // JSON object expected, with single key/value array "extensions":[{},..]
    assertTrue(result.content.startsWith("{\"extensions\":["));
    assertEquals(1, rootNode.size());

    JsonNode extensionsNode = rootNode.get("extensions");
    assertEquals(18, extensionsNode.size());
    assertNotNull(extensionsNode.get(0).get("title"));
    assertNotNull(extensionsNode.get(0).get("description"));
    assertNotNull(extensionsNode.get(0).get("subject"));
    assertNotNull(extensionsNode.get(0).get("identifier"));
    assertNotNull(extensionsNode.get(0).get("url"));
  }
}
