package org.gbif.registry.ws.resources.collections;

import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.ws.guice.Trim;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Resolves the grscicoll identifiers to the corresponding entity ({@link
 * org.gbif.api.model.collections.Collection} or {@link
 * org.gbif.api.model.collections.Institution}).
 */
@Singleton
@Path(GRSCICOLL_PATH + "/resolve")
public class IdentifierResolverResource {

  private final String grscicollPortalUrl;
  private final CollectionMapper collectionMapper;
  private final InstitutionMapper institutionMapper;

  @Inject
  public IdentifierResolverResource(
      @Named("grscicollPortalUrl") String grscicollPortalUrl,
      CollectionMapper collectionMapper,
      InstitutionMapper institutionMapper) {
    this.collectionMapper = collectionMapper;
    this.grscicollPortalUrl = grscicollPortalUrl;
    this.institutionMapper = institutionMapper;
  }

  @GET
  @Path("{env: .*}{identifier: (grscicoll.org|grbio.org|biocol.org)/.+}")
  public Response resolveGrbioBiocolUris(
      @PathParam("identifier") @NotNull @Trim String identifier) {
    return processIdentifier(identifier);
  }

  @GET
  @Path("{identifier}")
  public Response resolve(@PathParam("identifier") @NotNull @Trim String identifier) {
    return processIdentifier(identifier);
  }

  private Response processIdentifier(String identifier) {
    Optional<String> entityPath = findEntityPath(identifier);

    if (!entityPath.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.seeOther(URI.create(grscicollPortalUrl + entityPath.get())).build();
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
