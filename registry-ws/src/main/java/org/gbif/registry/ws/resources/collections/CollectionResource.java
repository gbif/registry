package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("grbio/collection")
public class CollectionResource extends BaseExtendableCollectionResource<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;

  @Inject
  public CollectionResource(CollectionMapper collectionMapper, AddressMapper addressMapper,
                            IdentifierMapper identifierMapper,TagMapper tagMapper) {
    super(collectionMapper, addressMapper, collectionMapper, tagMapper, collectionMapper, identifierMapper, collectionMapper);
    this.collectionMapper = collectionMapper;
  }

  @GET
  public PagingResponse<Collection> list(@Nullable @QueryParam("q") String query,
                                         @Nullable @QueryParam("institution") UUID institutionKey,
                                         @Nullable @Context Pageable page) {
    long total = collectionMapper.count(institutionKey, query);
    return new PagingResponse<>(page, total, collectionMapper.list(institutionKey, query, page));
  }
}
