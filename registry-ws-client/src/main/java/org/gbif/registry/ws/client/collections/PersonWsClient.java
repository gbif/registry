package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class PersonWsClient extends BaseCrudClient<Person> implements PersonService {

  private static final GenericType<PagingResponse<Person>> PAGING_PERSON = new GenericType<PagingResponse<Person>>() {};

  @Inject
  protected PersonWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Person.class, resource.path("grbio/person"), authFilter, PAGING_PERSON);
  }

  @Override
  public PagingResponse<Person> list(
    @Nullable String query, @Nullable UUID institutionKey, @Nullable UUID collectionKey, @Nullable Pageable pageable
  ) {
    return get(PAGING_PERSON,
               null,
               QueryParamBuilder.create("institution", institutionKey)
                 .queryParam("collection", collectionKey)
                 .queryParam("q", query)
                 .build(),
               pageable);
  }
}
