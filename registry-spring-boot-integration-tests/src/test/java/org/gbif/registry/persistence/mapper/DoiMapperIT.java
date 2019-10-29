package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class DoiMapperIT {

  @Autowired
  private DoiMapper mapper;

  @Test
  public void testCreate() {
    DOI doi = new DOI("10.998/dead.moon");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DOWNLOAD);
    DoiData data = mapper.get(doi);
    assertNotNull(data);
    assertEquals(DoiStatus.NEW, data.getStatus());
    assertNull(data.getTarget());
  }

  @Test
  public void testList() {
    DOI doi = new DOI("10.998/dead.pool");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DATASET);
    List<Map<String, Object>> data = mapper.list(null, DoiType.DATASET, null);

    assertNotNull(data);
    assertEquals(1, data.size());
    //assertNull(data.getTarget());
  }

  @Test
  public void testUpdate() {
    DOI doi = new DOI("10.998/dead.kennedys");
    mapper.create(doi, DoiType.DOWNLOAD);

    mapper.update(doi, new DoiData(DoiStatus.NEW, null), null);
    DoiData data = mapper.get(doi);
    assertEquals(DoiStatus.NEW, data.getStatus());
    assertNull(data.getTarget());

    mapper.update(doi, new DoiData(DoiStatus.RESERVED, null), null);
    data = mapper.get(doi);
    assertEquals(DoiStatus.RESERVED, data.getStatus());
    assertNull(data.getTarget());

    URI uri = URI.create("ftp://ftp.bands.com");
    mapper.update(doi, new DoiData(DoiStatus.REGISTERED, uri), null);
    data = mapper.get(doi);
    assertEquals(DoiStatus.REGISTERED, data.getStatus());
    assertEquals(uri, data.getTarget());
  }

  @Test
  public void testDelete() {
    DOI doi = new DOI("10.998/dead.kennedys");
    mapper.create(doi, DoiType.DOWNLOAD);
    assertNotNull(mapper.get(doi));
    mapper.delete(doi);
    assertNull(mapper.get(doi));
  }
}
