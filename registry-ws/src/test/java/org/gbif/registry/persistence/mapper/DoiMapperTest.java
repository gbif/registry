package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.guice.RegistryTestModules;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DoiMapperTest {

  private DoiMapper mapper;

  @ClassRule
  public static LiquibaseInitializer liquibase = new LiquibaseInitializer(RegistryTestModules.database());

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.mybatis();
    mapper = inj.getInstance(DoiMapper.class);
  }

  @Test
  public void testCreate() throws Exception {
    DOI doi = new DOI("10.998/dead.moon");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DOWNLOAD);
    DoiData data = mapper.get(doi);
    assertNotNull(data);
    assertEquals(DoiStatus.NEW, data.getStatus());
    assertNull(data.getTarget());
  }

  @Test
  public void testList() throws Exception {
    DOI doi = new DOI("10.998/dead.pool");
    assertNull(mapper.get(doi));
    mapper.create(doi, DoiType.DATASET);
    List<Map<String, Object>> data = mapper.list(null, DoiType.DATASET, null);

    assertNotNull(data);
    assertEquals(1, data.size());
    //assertNull(data.getTarget());
  }

  @Test
  public void testUpdate() throws Exception {
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
  public void testDelete() throws Exception {
    DOI doi = new DOI("10.998/dead.kennedys");
    mapper.create(doi, DoiType.DOWNLOAD);
    assertNotNull(mapper.get(doi));
    mapper.delete(doi);
    assertNull(mapper.get(doi));
  }
}