package org.gbif.registry.metadata.parse;

import com.google.common.io.Closeables;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.License;
import org.gbif.common.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.gbif.common.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test class for development use, to parse metadata documents from the registry.
 */
public class DatasetParserManualIT {

  Map<String,License> ok = new HashMap<>();
  Set<String> fail = new HashSet<>();

  @Test
  @Ignore
  public void testOne() {
    testLicense("3920856d-4923-4276-ae0b-e8b3478df276");
  }

  @Test
  @Ignore
  public void parseListOfDatasets() throws Exception {
    File uuids = new File("dataset-uuids-to-check");

    int i = 0;
    try (BufferedReader br = new BufferedReader(new FileReader(uuids))) {
      String line;
      while ((line = br.readLine()) != null) {
        i++;
        System.out.println(i + " " + line);
        testLicense(line.trim());
        System.out.println();
      }
    }

    BufferedWriter bw = new BufferedWriter(new FileWriter(new File("dataset-licenses")));

    for (Map.Entry<String,License> e : ok.entrySet()) {
      System.out.println(e.getKey() + " → " + e.getValue());
      bw.append(e.getKey() + "\t" + e.getValue() + "\n");
    }

    for (String k : fail) {
      System.out.println(k + " → fail");
      bw.append(k + "\tfail\n");
    }

    bw.close();
  }

  public void testLicense(String key) {

    InputStream stream = null;
    try {
      stream = getMetadataDocument(key);
      Dataset d = DatasetParser.build(stream);

      System.out.println("License for "+key+" is "+d.getLicense());
      ok.put(key, d.getLicense());

    } catch (Exception e) {
      System.out.println("No metadata document for "+key);
      fail.add(key);
    } finally {
      Closeables.closeQuietly(stream);
    }
  }

  public InputStream getMetadataDocument(String key) throws Exception {
    URL ds = new URL("https://api.gbif.org/v1/dataset/"+key+"/metadata");
    HttpURLConnection con = (HttpURLConnection) ds.openConnection();
    con.setRequestMethod("GET");

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer content = new StringBuffer();
    while ((inputLine = in.readLine()) != null) {
      content.append(inputLine);
    }
    in.close();

    // Find the metadata document key
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(content.toString());
    int mdkey = rootNode.get(0).get("key").asInt();

    // Retrieve the metadata document
    URL metadata = new URL("https://api.gbif.org/v1/dataset/metadata/"+mdkey+"/document");
    HttpURLConnection con2 = (HttpURLConnection) metadata.openConnection();
    con2.setRequestMethod("GET");

    return con2.getInputStream();
  }
}
