package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRightsMapper {

  boolean keyExistsForUser(@Param("username") String username, @Param("key") UUID key);

  boolean namespaceExistsForUser(@Param("username") String username, @Param("ns") String namespace);

  boolean allowedToDeleteMachineTag(@Param("username") String username, @Param("key") int machineTagKey);
}
