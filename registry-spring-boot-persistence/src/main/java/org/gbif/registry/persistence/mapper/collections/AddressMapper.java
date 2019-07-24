package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Address;
import org.springframework.stereotype.Repository;

/** Mapper for collections-related {@link Address}. */
@Repository
public interface AddressMapper {

  void create(Address address);

  void update(Address address);

  void delete(Integer key);
}
