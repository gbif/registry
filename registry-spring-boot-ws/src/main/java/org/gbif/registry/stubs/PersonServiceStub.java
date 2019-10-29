package org.gbif.registry.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Person;
import org.gbif.api.service.directory.PersonService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Service
public class PersonServiceStub implements PersonService {
  @Override
  public Person create(@NotNull Person person) {
    throw new UnsupportedOperationException("PersonService#create not implemented yet (directory-ws needed)");
  }

  @Override
  public Person get(@NotNull Integer integer) {
    throw new UnsupportedOperationException("PersonService#get not implemented yet (directory-ws needed)");
  }

  @Override
  public void update(@NotNull Person person) {
    throw new UnsupportedOperationException("PersonService#update not implemented yet (directory-ws needed)");
  }

  @Override
  public void delete(@NotNull Integer integer) {
    throw new UnsupportedOperationException("PersonService#delete not implemented yet (directory-ws needed)");
  }

  @Override
  public PagingResponse<Person> list(@Nullable String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("PersonService#list not implemented yet (directory-ws needed)");
  }
}
