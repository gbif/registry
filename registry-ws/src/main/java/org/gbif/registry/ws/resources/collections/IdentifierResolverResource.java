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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.Trim;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Resolves the GRSciColl identifiers to the corresponding entity ({@link
 * org.gbif.api.model.collections.Collection} or {@link
 * org.gbif.api.model.collections.Institution}).
 *
 * <p>This controller receives as parameter URLs like http://grbio.org/cool/gd90-pmbb and responds
 * with a redirect to the corresponding page of the collection entity on the GBIF portal.
 */
@Hidden
@Validated
@RestController
@RequestMapping(GRSCICOLL_PATH + "/resolve")
public class IdentifierResolverResource {

  // resolve only the following ones: grbio.org, biocol.org, grscicoll.org, usfsc.grscicoll.org
  // url may start with an environment string (env or uat)
  private static final Pattern PATTERN =
      Pattern.compile(
          "(dev\\.|uat\\.)*(.*[grbio\\.org|biocol\\.org|grscicoll\\.org|usfsc\\.grscicoll\\.org].*)");

  private final String grscicollPortalUrl;
  private final CollectionMapper collectionMapper;
  private final InstitutionMapper institutionMapper;

  public IdentifierResolverResource(
      @Value("${grscicoll.portal.url}") String grscicollPortalUrl,
      CollectionMapper collectionMapper,
      InstitutionMapper institutionMapper) {
    this.collectionMapper = collectionMapper;
    this.grscicollPortalUrl = grscicollPortalUrl;
    this.institutionMapper = institutionMapper;
  }

  @GetMapping
  public ResponseEntity<Void> resolve(
      HttpServletRequest request, @RequestParam("identifier") @Trim String identifier) {
    final Matcher matcher = PATTERN.matcher(identifier);

    if (matcher.matches()) {
      // we just ignore group(1) env
      return processIdentifier(matcher.group(2));
    }

    return ResponseEntity.notFound().build();
  }

  private ResponseEntity<Void> processIdentifier(String identifier) {
    Optional<String> entityPath = findEntityPath(identifier);

    return entityPath
        .map(
            path ->
                ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .location(URI.create(grscicollPortalUrl + path))
                    .<Void>build())
        .orElse(ResponseEntity.notFound().build());
  }

  private Optional<String> findEntityPath(String identifier) {
    List<UUID> keys = institutionMapper.findByIdentifier(identifier);
    if (keys != null && !keys.isEmpty()) {
      return Optional.of("institution/" + keys.get(0));
    }

    keys = collectionMapper.findByIdentifier(identifier);
    if (keys != null && !keys.isEmpty()) {
      return Optional.of("collection/" + keys.get(0));
    }

    return Optional.empty();
  }
}
