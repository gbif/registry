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
package org.gbif.registry.ws.it.security;

import org.gbif.registry.security.SecurityContextCheck;

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link SecurityContextCheck}. */
public class SecurityContextCheckTest {

  @Test
  public void testCheckUserInRole() {
    assertTrue(SecurityContextCheck.checkUserInRole(new TestSecurityContext("A"), "A"));
    assertFalse(SecurityContextCheck.checkUserInRole(new TestSecurityContext("A"), "B"));

    // test empty role
    assertFalse(SecurityContextCheck.checkUserInRole(new TestSecurityContext("A"), null));
    assertFalse(SecurityContextCheck.checkUserInRole(new TestSecurityContext("A"), ""));

    // test multiple roles
    assertTrue(SecurityContextCheck.checkUserInRole(new TestSecurityContext("A"), "C", "A"));
    assertFalse(SecurityContextCheck.checkUserInRole(new TestSecurityContext("A"), "B", "C"));
  }

  /** Test implementation of {@link SecurityContext} */
  private static class TestSecurityContext implements Authentication {

    String role;

    TestSecurityContext(String role) {
      this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role);
      return Collections.singleton(simpleGrantedAuthority);
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return null;
    }

    @Override
    public boolean isAuthenticated() {
      return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

    @Override
    public String getName() {
      return null;
    }
  }
}
