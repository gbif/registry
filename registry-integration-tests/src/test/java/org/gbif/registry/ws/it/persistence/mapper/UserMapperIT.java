package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserMapperIT extends BaseItTest {

  private final UserMapper mapper;

  @Autowired
  public UserMapperIT(
      UserMapper mapper, SimplePrincipalProvider principalProvider, EsManageServer esServer) {
    super(principalProvider, esServer);
    this.mapper = mapper;
  }

  @Test
  public void testCreate() {
    GbifUser user = new GbifUser();
    user.setUserName("test_username");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("test_username@gbif.org");
    user.setPasswordHash("$S$DtF4Xp0rohjeVvFuA7TOkfLsdhXnDMqfRjNvU.5g9468TPQojcPA");
    Set<UserRole> roles = new HashSet<>();
    roles.add(UserRole.USER);
    user.setRoles(roles);
    Map<String, String> settings = new HashMap<>();
    settings.put("country", "DK");
    user.setSettings(settings);
    user.setLocale(new Locale("en"));
    assertNull(mapper.get(user.getUserName()));

    mapper.create(user);
    GbifUser created = mapper.get(user.getUserName());
    assertNotNull(created);
    assertEquals(new Locale("en"), created.getLocale());
  }
}
