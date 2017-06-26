package org.gbif.registry.ws.security;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Unit tests for {@link SecurityContextCheck}.
 */
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

  /**
   * Test implementation of {@link SecurityContext}
   */
  private static class TestSecurityContext implements SecurityContext {

    String role;
    TestSecurityContext(String role) {
      this.role = role;
    }

    @Override
    public Principal getUserPrincipal() {
      return null;
    }

    @Override
    public boolean isUserInRole(String s) {
      return role.equals(s);
    }

    @Override
    public boolean isSecure() {
      return false;
    }

    @Override
    public String getAuthenticationScheme() {
      return null;
    }
  }
}
