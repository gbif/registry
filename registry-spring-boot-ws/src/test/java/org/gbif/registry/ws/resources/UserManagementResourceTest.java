package org.gbif.registry.ws.resources;

import org.gbif.api.service.common.IdentityService;
import org.gbif.registry.domain.ws.UserAdminView;
import org.gbif.ws.security.AppkeysConfiguration;
import org.gbif.ws.security.GbifAuthentication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UserManagementResourceTest {

  @Mock
  private GbifAuthentication mockAuth;
  @Mock
  private IdentityService mockIdentityService;
  @Mock
  private AppkeysConfiguration appkeysConfiguration;
  @InjectMocks
  private UserManagementResource resource;

  @Test
  public void testGetUserWhenUserNotExistResponseShouldBeNull() {
    // WHEN
    UserAdminView response = resource.getUser("user");

    // THEN
    assertNull(response);
    verify(mockIdentityService).get("user");
  }

  @Test
  public void testGetUserBySystemSettingsWhenUserNotExistResponseShouldBeNull() {
    // WHEN
    UserAdminView response = resource.getUserBySystemSetting(Collections.emptyMap());

    // THEN
    assertNull(response);
  }
}
