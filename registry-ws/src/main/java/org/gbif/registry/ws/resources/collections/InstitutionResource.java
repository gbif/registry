package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;

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
@Path("institution")
public class InstitutionResource extends BaseCollectionResource<Institution>
    implements InstitutionService {

  @Inject
  public InstitutionResource(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper) {
    super(
        institutionMapper,
        addressMapper,
        institutionMapper,
        tagMapper,
        institutionMapper,
        identifierMapper,
        institutionMapper);
  }

  @GET
  public PagingResponse<Institution> list(
      @Nullable @QueryParam("q") String query, @Nullable @Context Pageable page) {
    return Strings.isNullOrEmpty(query) ? list(page) : search(query, page);
  }
}
