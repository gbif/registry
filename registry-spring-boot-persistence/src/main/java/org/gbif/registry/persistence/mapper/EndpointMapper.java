package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Endpoint;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointMapper {

  int createEndpoint(Endpoint endpoint);

  void addMachineTag(@Param("endpointKey") int endpointKey, @Param("machineTagKey") int machineTagKey);
}
