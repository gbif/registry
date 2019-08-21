package org.gbif.registry.ws.security;

import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthentication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurityContextCheckTest {

  @Mock
  private GbifAuthentication mockAuth;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    Collection mockUserRoles = Arrays.asList(
        new SimpleGrantedAuthority("A"),
        new SimpleGrantedAuthority("B"),
        new SimpleGrantedAuthority("C")
    );
    when(mockAuth.getAuthorities()).thenReturn(mockUserRoles);
  }

  @Test
  public void testCheckUserInOneRole() {
    assertTrue(SecurityContextCheck.checkUserInRole(mockAuth, "A"));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth, "D"));
    assertTrue(SecurityContextCheck.checkUserInRole(mockAuth, "A", "B"));
    assertTrue(SecurityContextCheck.checkUserInRole(mockAuth, "D", "C"));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth, null));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth, ""));
  }

  @Test
  public void testEnsureGbifScheme() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureGbifScheme(mockAuth);

  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureGbifSchemeFail() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("Basic");
    SecurityContextCheck.ensureGbifScheme(mockAuth);
  }

  @Test
  public void testEnsureNotGbifScheme() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("Basic");
    SecurityContextCheck.ensureNotGbifScheme(mockAuth);

  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureNotGbifSchemeFail() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureNotGbifScheme(mockAuth);
  }

  @Test
  public void testEnsureUserSetInSecurityContext() {
    UserDetails mockUser = mock(UserDetails.class);
    when(mockUser.getUsername()).thenReturn("anonymous");
    when(mockAuth.getPrincipal()).thenReturn(mockUser);
    SecurityContextCheck.ensureUserSetInSecurityContext(mockAuth);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureUserSetInSecurityContextFailNullAuthentication() {
    SecurityContextCheck.ensureUserSetInSecurityContext(null);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureUserSetInSecurityContextFailNullPrincipal() {
    SecurityContextCheck.ensureUserSetInSecurityContext(mockAuth);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureUserSetInSecurityContextFailNullUsername() {
    UserDetails mockUser = mock(UserDetails.class);
    when(mockAuth.getPrincipal()).thenReturn(mockUser);
    SecurityContextCheck.ensureUserSetInSecurityContext(mockAuth);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureAuthorizedUserImpersonationNotGbifScheme() {
    UserDetails mockUser = mock(UserDetails.class);
    when(mockUser.getUsername()).thenReturn("anonymous");
    when(mockAuth.getPrincipal()).thenReturn(mockUser);
    SecurityContextCheck.ensureAuthorizedUserImpersonation(mockAuth, "Basic xxx", Arrays.asList("xx", "xxx"));
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureAuthorizedUserImpersonationNoUserInContext() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureAuthorizedUserImpersonation(mockAuth, "GBIF xxx:zzz", Arrays.asList("xx", "xxx"));
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureAuthorizedUserImpersonationNoSuchAppKeyInWhiteList() {
    UserDetails mockUser = mock(UserDetails.class);
    when(mockUser.getUsername()).thenReturn("anonymous");
    when(mockAuth.getPrincipal()).thenReturn(mockUser);
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureAuthorizedUserImpersonation(mockAuth, "GBIF yyy:zzz", Arrays.asList("xx", "xxx"));
  }

  @Test
  public void testEnsureAuthorizedUserImpersonation() {
    UserDetails mockUser = mock(UserDetails.class);
    when(mockUser.getUsername()).thenReturn("anonymous");
    when(mockAuth.getPrincipal()).thenReturn(mockUser);
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureAuthorizedUserImpersonation(mockAuth, "GBIF xxx:zzz", Arrays.asList("xx", "xxx"));
  }

  @Test
  public void testEnsurePrecondition() {
    SecurityContextCheck.ensurePrecondition(true, HttpStatus.NOT_FOUND);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsurePreconditionFail() {
    SecurityContextCheck.ensurePrecondition(false, HttpStatus.NOT_FOUND);
  }
}