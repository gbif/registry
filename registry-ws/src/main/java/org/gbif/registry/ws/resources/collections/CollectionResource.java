package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
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

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("collection")
public class CollectionResource extends BaseCollectionResource<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;

  @Inject
  public CollectionResource(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper) {
    super(
        collectionMapper,
        addressMapper,
        collectionMapper,
        tagMapper,
        collectionMapper,
        identifierMapper,
        collectionMapper);
    this.collectionMapper = collectionMapper;
  }

  @GET
  public PagingResponse<Collection> list(
      @Nullable @QueryParam("q") String query,
      @Nullable @QueryParam("institution") UUID institutionKey,
      @Nullable @Context Pageable page) {
    if (institutionKey != null) {
      return listByInstitution(institutionKey, page);
    } else if (!Strings.isNullOrEmpty(query)) {
      return search(query, page);
    } else {
      return list(page);
    }
  }

  @Override
  public PagingResponse<Collection> listByInstitution(
      UUID institutionKey, @Nullable Pageable pageable) {
    pageable = pageable == null ? new PagingRequest() : pageable;
    long total = collectionMapper.count();

    return new PagingResponse<>(
        pageable.getOffset(),
        pageable.getLimit(),
        total,
        collectionMapper.listCollectionsByInstitution(institutionKey, pageable));
  }
}
