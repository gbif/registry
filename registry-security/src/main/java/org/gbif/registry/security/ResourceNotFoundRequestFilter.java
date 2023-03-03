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

import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.ws.WebApplicationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
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

@Component
public class ResourceNotFoundRequestFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceNotFoundRequestFilter.class);

  private static final Pattern ENTITY_PATTERN =
      Pattern.compile(
          ".*/(organization|dataset|installation|node|network|institution|collection)/([a-f0-9-]+)/.+$");

  Map<String, Predicate<UUID>> ENTITY_EXISTS_PREDICATES = new HashMap<>();

  public ResourceNotFoundRequestFilter(
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      NodeMapper nodeMapper,
      NetworkMapper networkMapper,
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper) {
    ENTITY_EXISTS_PREDICATES.put("organization", key -> organizationMapper.get(key) != null);
    ENTITY_EXISTS_PREDICATES.put("dataset", key -> datasetMapper.get(key) != null);
    ENTITY_EXISTS_PREDICATES.put("installation", key -> installationMapper.get(key) != null);
    ENTITY_EXISTS_PREDICATES.put("node", key -> nodeMapper.get(key) != null);
    ENTITY_EXISTS_PREDICATES.put("network", key -> networkMapper.get(key) != null);
    ENTITY_EXISTS_PREDICATES.put("institution", key -> institutionMapper.get(key) != null);
    ENTITY_EXISTS_PREDICATES.put("collection", key -> collectionMapper.get(key) != null);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    Matcher entityMatcher = ENTITY_PATTERN.matcher(request.getRequestURI());
    if (request.getMethod().equalsIgnoreCase("GET") && entityMatcher.matches()) {
      String entityType = entityMatcher.group(1);

      UUID key = null;
      try {
        key = UUID.fromString(entityMatcher.group(2));
      } catch (Exception ex) {
        LOG.info("Not an entity key. Skipping request", ex);
      }

      if (key != null && !ENTITY_EXISTS_PREDICATES.get(entityType).test(key)) {
        throw new WebApplicationException("Entity not found", HttpStatus.NOT_FOUND);
      }
    }

    filterChain.doFilter(request, response);
  }
}
