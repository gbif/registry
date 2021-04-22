package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultCollectionService extends ExtendedCollectionService<Collection> {

  @Autowired
  protected DefaultCollectionService(
      BaseMapper<Collection> baseMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper) {
    super(baseMapper, addressMapper, machineTagMapper, tagMapper, identifierMapper);
  }
}
