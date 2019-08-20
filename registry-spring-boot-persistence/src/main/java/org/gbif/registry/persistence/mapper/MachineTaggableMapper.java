package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface MachineTaggableMapper<T extends MachineTaggable> {

  int addMachineTag(@Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey);

  int deleteMachineTag(@Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey);

  int deleteMachineTags(@Param("targetEntityKey") UUID entityKey, @Param("namespace") String namespace, @Nullable @Param("name") String name);

  List<MachineTag> listMachineTags(@Param("targetEntityKey") UUID targetEntityKey);

  long countByMachineTag(@Param("namespace") String namespace, @Nullable @Param("name") String name, @Nullable @Param("value") String value);

  List<T> listByMachineTag(@Param("namespace") String namespace, @Nullable @Param("name") String name, @Nullable @Param("value") String value, @Param("page") Pageable page);
}
