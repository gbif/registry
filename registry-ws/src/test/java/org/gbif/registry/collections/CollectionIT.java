package org.gbif.registry.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.TagService;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CollectionIT extends BaseCollectionTest<Collection> {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";

  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
        new Object[] {webservice.getInstance(CollectionResource.class), null},
        new Object[] {
          client.getInstance(CollectionService.class),
          client.getInstance(SimplePrincipalProvider.class)
        });
  }

  public CollectionIT(CollectionService collectionService, @Nullable SimplePrincipalProvider pp) {
    super(collectionService, collectionService, collectionService, collectionService, pp);
  }

  @Override
  protected Collection newEntity() {
    Collection collection = new Collection();
    collection.setCode(CODE);
    collection.setName(NAME);
    collection.setDescription(DESCRIPTION);
    return collection;
  }

  @Override
  protected void assertNewEntity(Collection entity) {
    assertEquals(CODE, entity.getCode());
    assertEquals(NAME, entity.getName());
    assertEquals(DESCRIPTION, entity.getDescription());
  }

  @Override
  protected Collection updateEntity(Collection entity) {
    entity.setCode(CODE_UPDATED);
    entity.setName(NAME_UPDATED);
    entity.setDescription(DESCRIPTION_UPDATED);
    return entity;
  }

  @Override
  protected void assertUpdatedEntity(Collection entity) {
    assertEquals(CODE_UPDATED, entity.getCode());
    assertEquals(NAME_UPDATED, entity.getName());
    assertEquals(DESCRIPTION_UPDATED, entity.getDescription());
  }
}
