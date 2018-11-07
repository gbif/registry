package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Address;

/** Mapper for collections-related {@link Address}. */
public interface AddressMapper {

  void create(Address address);

  void update(Address address);
}
