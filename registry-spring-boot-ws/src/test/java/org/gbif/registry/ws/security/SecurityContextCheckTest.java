/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.security;

import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthentication;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurityContextCheckTest {

  @Mock private GbifAuthentication mockAuth;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    Collection mockUserRoles =
        Arrays.asList(
            new SimpleGrantedAuthority("A"),
            new SimpleGrantedAuthority("B"),
            new SimpleGrantedAuthority("C"));
    when(mockAuth.getAuthorities()).thenReturn(mockUserRoles);
  }

  @Test
  public void testCheckUserInOneRole() {
    assertTrue(SecurityContextCheck.checkUserInRole(mockAuth, "A"));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth, "D"));
    assertTrue(SecurityContextCheck.checkUserInRole(mockAuth, "A", "B"));
    assertTrue(SecurityContextCheck.checkUserInRole(mockAuth, "D", "C"));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth));
    assertFalse(SecurityContextCheck.checkUserInRole(mockAuth, ""));
  }

  @Test
  public void testEnsureGbifScheme() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureGbifScheme(mockAuth);
    verify(mockAuth).getAuthenticationScheme();
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureGbifSchemeFail() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("Basic");
    SecurityContextCheck.ensureGbifScheme(mockAuth);
    verify(mockAuth).getAuthenticationScheme();
  }

  @Test
  public void testEnsureNotGbifScheme() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("Basic");
    SecurityContextCheck.ensureNotGbifScheme(mockAuth);
    verify(mockAuth).getAuthenticationScheme();
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureNotGbifSchemeFail() {
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureNotGbifScheme(mockAuth);
    verify(mockAuth).getAuthenticationScheme();
  }

  @Test
  public void testEnsureUserSetInSecurityContext() {
    when(mockAuth.getName()).thenReturn("anonymous");
    SecurityContextCheck.ensureUserSetInSecurityContext(mockAuth);
    verify(mockAuth).getName();
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
  public void testEnsureUserSetInSecurityContextFailNullName() {
    SecurityContextCheck.ensureUserSetInSecurityContext(mockAuth);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureAuthorizedUserImpersonationNotGbifScheme() {
    when(mockAuth.getName()).thenReturn("anonymous");
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        mockAuth, "Basic xxx", Arrays.asList("xx", "xxx"));
    verify(mockAuth).getName();
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureAuthorizedUserImpersonationNoUserInContext() {
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        mockAuth, "GBIF xxx:zzz", Arrays.asList("xx", "xxx"));
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsureAuthorizedUserImpersonationNoSuchAppKeyInWhiteList() {
    when(mockAuth.getName()).thenReturn("anonymous");
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        mockAuth, "GBIF yyy:zzz", Arrays.asList("xx", "xxx"));
    verify(mockAuth).getName();
    verify(mockAuth).getAuthenticationScheme();
  }

  @Test
  public void testEnsureAuthorizedUserImpersonation() {
    when(mockAuth.getName()).thenReturn("anonymous");
    when(mockAuth.getAuthenticationScheme()).thenReturn("GBIF");
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        mockAuth, "GBIF xxx:zzz", Arrays.asList("xx", "xxx"));
    verify(mockAuth).getName();
    verify(mockAuth).getAuthenticationScheme();
  }

  @Test
  public void testEnsurePrecondition() throws WebApplicationException {
    SecurityContextCheck.ensurePrecondition(true, HttpStatus.NOT_FOUND);
  }

  @Test(expected = WebApplicationException.class)
  public void testEnsurePreconditionFail() {
    SecurityContextCheck.ensurePrecondition(false, HttpStatus.NOT_FOUND);
  }
}
