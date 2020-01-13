package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class PersonWsClient extends BaseCrudClient<Person> implements PersonService, IdentifierService {

  private static final GenericType<PagingResponse<Person>> PAGING_PERSON = new GenericType<PagingResponse<Person>>() {};
  private static final GenericType<List<PersonSuggestResult>> LIST_PERSON_SUGGEST =
    new GenericType<List<PersonSuggestResult>>() {};
  protected static final GenericType<List<Identifier>> LIST_IDENTIFIER =
    new GenericType<List<Identifier>>() {};

  @Inject
  protected PersonWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Person.class, resource.path("grscicoll/person"), authFilter, PAGING_PERSON);
  }

  @Override
  public PagingResponse<Person> list(
    @Nullable String query, @Nullable UUID institutionKey, @Nullable UUID collectionKey, @Nullable Pageable pageable
  ) {
    return get(PAGING_PERSON,
               null,
               QueryParamBuilder.create("primaryInstitution", institutionKey)
                 .queryParam("primaryCollection", collectionKey)
                 .queryParam("q", query)
                 .build(),
               pageable);
  }

  @Override
  public PagingResponse<Person> listDeleted(@Nullable Pageable pageable) {
    return get(PAGING_PERSON, pageable, "deleted");
  }

  @Override
  public List<PersonSuggestResult> suggest(@Nullable String q) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.putSingle("q", q);
    return get(LIST_PERSON_SUGGEST, null, queryParams, null, "suggest");
  }

  @Override
  public int addIdentifier(@NotNull UUID key, @NotNull Identifier identifier) {
    return post(Integer.class, identifier, String.valueOf(key), "identifier");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID key, int identifierKey) {
    delete(String.valueOf(key), "identifier", String.valueOf(identifierKey));
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID key) {
    return get(LIST_IDENTIFIER, null, null, (Pageable) null, String.valueOf(key), "identifier");
  }
}
