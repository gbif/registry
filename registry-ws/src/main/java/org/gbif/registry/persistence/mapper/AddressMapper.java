package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;

public interface AddressMapper {

  void create(Address address);

  void update(Address address);

}
