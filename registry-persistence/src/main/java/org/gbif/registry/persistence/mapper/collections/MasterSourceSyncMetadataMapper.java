package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.MasterSourceMetadata;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterSourceSyncMetadataMapper {

  void create(MasterSourceMetadata masterSourceMetadata);

  void delete(@Param("key") int key);
}
