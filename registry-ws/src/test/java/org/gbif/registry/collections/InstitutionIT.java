package org.gbif.registry.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.StaffService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.registry.ws.resources.collections.StaffResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Arrays;
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
import static org.junit.Assert.assertNull;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InstitutionIT extends BaseCollectionTest<Institution> {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final URI HOMEPAGE = URI.create("http://dummy");
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final String ADDITIONAL_NAME = "additional name";

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
        new Object[] {
          webservice.getInstance(InstitutionResource.class),
          webservice.getInstance(StaffResource.class),
          null
        },
        new Object[] {
          client.getInstance(InstitutionService.class),
          client.getInstance(StaffService.class),
          client.getInstance(SimplePrincipalProvider.class)
        });
  }

  public InstitutionIT(
      InstitutionService institutionService,
      StaffService staffService,
      @Nullable SimplePrincipalProvider pp) {
    super(
        institutionService,
        institutionService,
        institutionService,
        institutionService,
        staffService,
        pp);
  }

  @Override
  protected Institution newEntity() {
    Institution institution = new Institution();
    institution.setCode(CODE);
    institution.setName(NAME);
    institution.setDescription(DESCRIPTION);
    institution.setHomepage(HOMEPAGE);
    return institution;
  }

  @Override
  protected void assertNewEntity(Institution institution) {
    assertEquals(CODE, institution.getCode());
    assertEquals(NAME, institution.getName());
    assertEquals(DESCRIPTION, institution.getDescription());
    assertEquals(HOMEPAGE, institution.getHomepage());
    assertNull(institution.getAdditionalNames());
  }

  @Override
  protected Institution updateEntity(Institution institution) {
    institution.setCode(CODE_UPDATED);
    institution.setName(NAME_UPDATED);
    institution.setDescription(DESCRIPTION_UPDATED);
    institution.setAdditionalNames(Arrays.asList(ADDITIONAL_NAME));
    return institution;
  }

  @Override
  protected void assertUpdatedEntity(Institution entity) {
    assertEquals(CODE_UPDATED, entity.getCode());
    assertEquals(NAME_UPDATED, entity.getName());
    assertEquals(DESCRIPTION_UPDATED, entity.getDescription());
    assertEquals(1, entity.getAdditionalNames().size());
  }

  @Override
  protected Institution newInvalidEntity() {
    return new Institution();
  }
}
