package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Person;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultPersonService extends BaseCollectionsService<Person> {

  @Autowired
  protected DefaultPersonService(BaseMapper<Person> baseMapper) {
    super(baseMapper);
  }

  @Override
  protected void update(Person entity) {
    // TODO
  }
}
