package org.gbif.registry.persistence.mapper.collections;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.descriptors.DescriptorSet;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorSetParams;
import org.springframework.stereotype.Repository;

@Repository
public interface DescriptorsMapper {

  DescriptorSet getDescriptorSet(@Param("key") long key);

  void createDescriptorSet(DescriptorSet entity);

  void deleteDescriptorSet(@Param("key") long key);

  void updateDescriptorSet(DescriptorSet entity);

  List<DescriptorSet> listDescriptorSets(@Param("params") DescriptorSetParams searchParams);

  long countDescriptorSets(@Param("params") DescriptorSetParams searchParams);

  DescriptorDto getDescriptor(@Param("key") long key);

  void createDescriptor(DescriptorDto entity);

  void deleteDescriptor(@Param("key") long key);

  List<DescriptorDto> listDescriptors(@Param("params") DescriptorParams searchParams);

  long countDescriptors(@Param("params") DescriptorParams searchParams);

  void deleteDescriptors(@Param("descriptorSetKey") long descriptorSetKey);

  void createVerbatim(
      @Param("descriptorKey") long descriptorKey,
      @Param("fieldName") String fieldName,
      @Param("fieldValue") String fieldValue);

  // TODO: list deleted
}
