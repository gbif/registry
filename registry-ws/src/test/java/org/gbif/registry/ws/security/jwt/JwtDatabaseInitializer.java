package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.fixtures.TestConstants;

import java.util.Set;
import javax.sql.DataSource;

import com.google.common.collect.Sets;
import com.google.inject.Injector;

/**
 * DB initialization needed for JWT tests.
 */
public class JwtDatabaseInitializer extends DatabaseInitializer {

  static final String ADMIN_USER = "administrator";
  static final String TEST_USER = "testuser";
  static final String GRSCICOLL_ADMIN = "grscicolladmin";

  private final IdentityService identityService;

  public JwtDatabaseInitializer(DataSource dataSource) {
    super(dataSource);
    Injector injector = RegistryTestModules.identityMybatis();
    identityService = injector.getInstance(IdentityService.class);
  }

  @Override
  protected void before() throws Throwable {
    // clean db
    super.before();

    // add users
    createUser(ADMIN_USER, Sets.newHashSet(UserRole.USER, UserRole.REGISTRY_ADMIN, UserRole.REGISTRY_EDITOR));
    createUser(TEST_USER, Sets.newHashSet(UserRole.USER));
    createUser(GRSCICOLL_ADMIN, Sets.newHashSet(UserRole.GRSCICOLL_ADMIN));
  }

  private void createUser(String username, Set<UserRole> roles) {
    GbifUser user = new GbifUser();
    user.setUserName(username);
    user.setFirstName(username);
    user.setLastName(username);
    user.setEmail(username + "@test.com");
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setRoles(roles);

    // password equals to username
    identityService.create(user, username);

    Integer key = identityService.get(username).getKey();
    identityService.updateLastLogin(key);
  }

}
