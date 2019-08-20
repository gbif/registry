package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Endpoint;

public interface EndpointMapper {

  int createEndpoint(Endpoint endpoint);

  void addMachineTag(@Param("endpointKey") int endpointKey, @Param("machineTagKey") int machineTagKey);

  // TODO: 2019-08-20 some methods are not implemented for some reason

}
