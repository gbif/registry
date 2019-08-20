package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.MachineTag;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineTagMapper {

  int createMachineTag(MachineTag machineTag);

}
