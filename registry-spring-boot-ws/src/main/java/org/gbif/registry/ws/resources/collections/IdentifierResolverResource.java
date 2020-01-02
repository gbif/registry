package org.gbif.registry.ws.resources.collections;

import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Resolves the grscicoll identifiers to the corresponding entity ({@link
 * org.gbif.api.model.collections.Collection} or {@link
 * org.gbif.api.model.collections.Institution}).
 */
@RestController
@RequestMapping(GRSCICOLL_PATH + "/resolve")
public class IdentifierResolverResource {

  // must match the service request mapping parameter
  private static final String RESOLVE_PARAM = "resolve/";

  // resolve only the following ones: grbio.org, biocol.org, grscicoll.org, usfsc.grscicoll.org
  // url may start with an environment string (env or uat)
  private static final Pattern PATTERN =
    Pattern.compile("(dev\\.|uat\\.)*(grbio\\.org|biocol\\.org.*|grscicoll\\.org.*|usfsc\\.grscicoll\\.org.*)");

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

  @GetMapping(value = "**")
  public ResponseEntity<Void> resolve(HttpServletRequest request) {
    final String fullRequestURI = request.getRequestURI();

    // url can probably begin with 'v1' or other
    final String requestURI = fullRequestURI.substring(fullRequestURI.indexOf(RESOLVE_PARAM) + RESOLVE_PARAM.length());

    final Matcher matcher = PATTERN.matcher(requestURI);

    if (PATTERN.matcher(requestURI).matches()) {
      // we just ignore group(1) env
      return processIdentifier(matcher.group(2));
    }

    return ResponseEntity.notFound().build();
  }

  private ResponseEntity<Void> processIdentifier(String identifier) {
    Optional<String> entityPath = findEntityPath(identifier);

    return entityPath.map(path -> ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(grscicollPortalUrl + path)).<Void>build())
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
