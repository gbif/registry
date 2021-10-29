/*
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
package org.gbif.registry.ws.util;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.ws.security.GbifUserPrincipal;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Sets;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityUtil {

  /**
   * Gets the GBIF User Principal from the currently authenticated user.
   *  Throws a SecurityException if no user is found in the current context.
   */
  public static GbifUserPrincipal getPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      if (authentication.getPrincipal() instanceof GbifUserPrincipal) {
        return (GbifUserPrincipal) authentication.getPrincipal();
      } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
        return toGbifUserPrincipal((UsernamePasswordAuthenticationToken) authentication);
      }
    }
    throw new SecurityException("User credentials not found");
  }

  /**
   * Mostly used for tests, it transforms a UsernamePasswordAuthenticationToken into a GBIF principal.
   */
  private GbifUserPrincipal toGbifUserPrincipal(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    GbifUser gbifUser = new GbifUser();
    gbifUser.setUserName(usernamePasswordAuthenticationToken.getName());
    gbifUser.setRoles(
        usernamePasswordAuthenticationToken.getAuthorities().stream()
            .map(ga -> UserRole.valueOf(ga.getAuthority()))
            .collect(Collectors.toSet()));
    return new GbifUserPrincipal(gbifUser);
  }

  /**
   * Is the  currently authenticated user the same as the username parameter.
   */
  public static boolean isAuthenticatedUser(String username) {
    return getPrincipal().getUsername().equals(username);
  }

  /**
   * Has the currently authenticated user the roles listed.
   */
  public static boolean isAuthenticatedUserInRole(String... roles) {
    Set<String> userRoles = Sets.newHashSet(roles);
    return getPrincipal().getAuthorities().stream()
        .anyMatch(grantedAuthority -> userRoles.contains(grantedAuthority.getAuthority()));
  }
}
