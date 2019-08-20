package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.MachineTag;

public interface MachineTagMapper {

  int createMachineTag(MachineTag machineTag);

}
