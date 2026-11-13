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
package org.gbif.registry.security.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Gives the request path the way Spring MVC routes it, so the authorization filters check the
 * same path that reaches the handler.
 *
 * <p>The raw {@link HttpServletRequest#getRequestURI()} must not be used for authorization:
 * Spring MVC decodes it ({@code %2D} is {@code -}), drops path parameters and ignores a trailing
 * slash, so a pattern that matches the raw URI can be bypassed by a variant that still routes to
 * the same handler.
 */
public final class RequestPathUtils {

  private static final Pattern SEMICOLON_CONTENT = Pattern.compile(";[^/]*");
  private static final Pattern REPEATED_SLASHES = Pattern.compile("/{2,}");

  private RequestPathUtils() {
    // private constructor
  }

  /**
   * Path within the application, URL decoded, without path parameters, repeated slashes or
   * trailing slash.
   */
  public static String normalizedPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (path != null && contextPath != null && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    return normalize(path);
  }

  /** Same steps as the URL decoding UrlPathHelper used for routing in WebMvcConfig. */
  static String normalize(String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }
    String normalized = SEMICOLON_CONTENT.matcher(path).replaceAll("");
    normalized = UriUtils.decode(normalized, StandardCharsets.UTF_8);
    normalized = REPEATED_SLASHES.matcher(normalized).replaceAll("/");
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
