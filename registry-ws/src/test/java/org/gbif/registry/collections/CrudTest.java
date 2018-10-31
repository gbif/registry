package org.gbif.registry.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CrudService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class CrudTest<T extends CollectionEntity> {

  protected static final BiFunction<Integer, Long, Pageable> PAGE =
    (limit, offset) ->
      new Pageable() {
        @Override
        public int getLimit() {
          return limit;
        }

        @Override
        public long getOffset() {
          return offset;
        }
      };

  private final CrudService<T> crudService;
  private final SimplePrincipalProvider pp;

  @ClassRule
  public static final LiquibaseInitializer liquibaseRule =
    new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule =
    new DatabaseInitializer(LiquibaseModules.database());

  public CrudTest(
    CrudService<T> crudService, @Nullable SimplePrincipalProvider pp) {
    this.crudService = crudService;
    this.pp = pp;
  }

  @Before
  public void setup() {
    if (pp != null) {
      pp.setPrincipal(TestConstants.TEST_ADMIN);
    }
  }

  protected abstract T newEntity();

  protected abstract void assertNewEntity(T entity);

  protected abstract T updateEntity(T entity);

  protected abstract void assertUpdatedEntity(T entity);

  @Test
  public void crudTest() {
    // create
    T entity = newEntity();

    UUID key = crudService.create(entity);
    assertNotNull(key);
    T entityStored = crudService.get(key);
    assertEquals(key, entityStored.getKey());
    assertNewEntity(entityStored);
    assertNotNull(entityStored.getCreatedBy());
    assertNotNull(entityStored.getCreated());
    assertNotNull(entityStored.getModifiedBy());
    assertNotNull(entityStored.getModified());

    // update
    entity = updateEntity(entityStored);
    crudService.update(entity);
    entityStored = crudService.get(key);
    assertUpdatedEntity(entityStored);

    // delete
    crudService.delete(key);
    entityStored = crudService.get(key);
    assertNotNull(entityStored.getDeleted());
  }

  @Test
  public void listTest() {
    T entity1 = newEntity();
    UUID key1 = crudService.create(entity1);

    T entity2 = newEntity();
    UUID key2 = crudService.create(entity2);

    T entity3 = newEntity();
    UUID key3 = crudService.create(entity3);

    PagingResponse<T> response = crudService.list(PAGE.apply(5, 0L));
    assertEquals(3, response.getResults().size());

    response = crudService.list(PAGE.apply(1, 0L));
    assertEquals(1, response.getResults().size());
  }

}
