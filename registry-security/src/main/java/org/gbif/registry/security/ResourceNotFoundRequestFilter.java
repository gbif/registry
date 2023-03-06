/* Licensed under the Apache License, Version 2.0 (the "License");
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

package org.gbif.registry.security;

import org.gbif.ws.WebApplicationException;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.gbif.registry.security.ResourceNotFoundService.Resource;

@Component
public class ResourceNotFoundRequestFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceNotFoundRequestFilter.class);

  private static final Pattern ENTITY_PATTERN =
      Pattern.compile(
          ".*/(organization|dataset|installation|node|network|institution|collection)/([a-f0-9-]+)/.+$");

  private final ResourceNotFoundService resourceNotFoundService;

  public ResourceNotFoundRequestFilter(ResourceNotFoundService resourceNotFoundService) {
    this.resourceNotFoundService = resourceNotFoundService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    Matcher entityMatcher = ENTITY_PATTERN.matcher(request.getRequestURI());
    if (request.getMethod().equalsIgnoreCase("GET") && entityMatcher.matches()) {
      Resource resource = Resource.fromString(entityMatcher.group(1).toUpperCase());

      UUID key = null;
      try {
        key = UUID.fromString(entityMatcher.group(2));
      } catch (Exception ex) {
        LOG.info("Not an entity key. Skipping request", ex);
      }

      if (resource != null && key != null && !resourceNotFoundService.entityExists(resource, key)) {
        throw new WebApplicationException("Entity not found", HttpStatus.NOT_FOUND);
      }
    }

    filterChain.doFilter(request, response);
  }
}
