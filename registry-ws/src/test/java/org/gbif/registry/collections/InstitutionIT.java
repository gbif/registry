package org.gbif.registry.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.function.BiFunction;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InstitutionIT extends BaseCollectionTest<Institution> {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
        new Object[] {webservice.getInstance(InstitutionResource.class), null},
        new Object[] {
          client.getInstance(InstitutionService.class),
          client.getInstance(SimplePrincipalProvider.class)
        });
  }

  public InstitutionIT(
      InstitutionService institutionService, @Nullable SimplePrincipalProvider pp) {
    super(institutionService, institutionService, institutionService, institutionService, pp);
  }

  @Override
  protected Institution newEntity() {
    Institution institution = new Institution();
    institution.setCode(CODE);
    institution.setName(NAME);
    institution.setDescription(DESCRIPTION);
    return institution;
  }

  @Override
  protected void assertNewEntity(Institution entity) {
    assertEquals(CODE, entity.getCode());
    assertEquals(NAME, entity.getName());
    assertEquals(DESCRIPTION, entity.getDescription());
  }

  @Override
  protected Institution updateEntity(Institution entity) {
    entity.setCode(CODE_UPDATED);
    entity.setName(NAME_UPDATED);
    entity.setDescription(DESCRIPTION_UPDATED);
    return entity;
  }

  @Override
  protected void assertUpdatedEntity(Institution entity) {
    assertEquals(CODE_UPDATED, entity.getCode());
    assertEquals(NAME_UPDATED, entity.getName());
    assertEquals(DESCRIPTION_UPDATED, entity.getDescription());
  }
}
