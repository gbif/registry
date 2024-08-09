package org.gbif.registry.persistence.mapper.collections;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.dto.VerbatimDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorGroupParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorParams;
import org.springframework.stereotype.Repository;

@Repository
public interface DescriptorsMapper {

  DescriptorGroup getDescriptorGroup(@Param("key") long key);

  void createDescriptorGroup(DescriptorGroup entity);

  void deleteDescriptorGroup(@Param("key") long key);

  void updateDescriptorGroup(DescriptorGroup entity);

  List<DescriptorGroup> listDescriptorGroups(@Param("params") DescriptorGroupParams searchParams);

  long countDescriptorGroups(@Param("params") DescriptorGroupParams searchParams);

  DescriptorDto getDescriptor(@Param("key") long key);

  void createDescriptor(DescriptorDto entity);

  void deleteDescriptor(@Param("key") long key);

  List<DescriptorDto> listDescriptors(@Param("params") DescriptorParams searchParams);

  long countDescriptors(@Param("params") DescriptorParams searchParams);

  void deleteDescriptors(@Param("descriptorGroupKey") long descriptorGroupKey);

  void createVerbatim(
      @Param("descriptorKey") long descriptorKey,
      @Param("fieldName") String fieldName,
      @Param("fieldValue") String fieldValue);

  // TODO: list deleted

  List<VerbatimDto> getVerbatimNames(long descriptorGroupKey);
}
