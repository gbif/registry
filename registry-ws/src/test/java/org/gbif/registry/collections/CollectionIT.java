package org.gbif.registry.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.StaffService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.TagService;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.registry.ws.resources.collections.StaffResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
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

  private final CollectionService collectionService;
  private final InstitutionService institutionService;

  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
        new Object[] {
          webservice.getInstance(CollectionResource.class),
          webservice.getInstance(InstitutionResource.class),
          webservice.getInstance(StaffResource.class),
          null
        },
        new Object[] {
          client.getInstance(CollectionService.class),
          client.getInstance(InstitutionService.class),
          client.getInstance(StaffService.class),
          client.getInstance(SimplePrincipalProvider.class)
        });
  }

  public CollectionIT(
      CollectionService collectionService,
      InstitutionService institutionService,
      StaffService staffService,
      @Nullable SimplePrincipalProvider pp) {
    super(collectionService, collectionService, collectionService, collectionService, staffService, pp);
    this.collectionService = collectionService;
    this.institutionService = institutionService;
  }

  @Test
  public void listByInstitutionTest() {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = institutionService.create(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = institutionService.create(institution2);

    // staff
    Collection collection1 = newEntity();
    collection1.setInstitutionKey(institutionKey1);
    collectionService.create(collection1);

    Collection collection2 = newEntity();
    collection2.setInstitutionKey(institutionKey1);
    collectionService.create(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    collectionService.create(collection3);

    PagingResponse<Collection> response =
        collectionService.listByInstitution(institutionKey1, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = collectionService.listByInstitution(institutionKey2, PAGE.apply(2, 0L));
    assertEquals(1, response.getResults().size());

    response = collectionService.listByInstitution(UUID.randomUUID(), PAGE.apply(2, 0L));
    assertEquals(0, response.getResults().size());
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
