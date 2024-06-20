package org.gbif.registry.persistence.mapper.collections;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorRecord;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorRecordDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorRecordsParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsParams;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DescriptorsMapper {

  Descriptor getDescriptor(@Param("key") long key);

  void createDescriptor(Descriptor entity);

  void deleteDescriptor(@Param("key") long key);

  void updateDescriptor(Descriptor entity);

  List<Descriptor> listDescriptors(@Param("params") DescriptorsParams searchParams);

  long countDescriptors(@Param("params") DescriptorsParams searchParams);

  DescriptorRecordDto getRecord(@Param("key") long key);

  void createRecord(DescriptorRecord entity);

  void deleteRecord(@Param("key") long key);

  // TODO: dto con verbatim y luego pasarlos a map
  List<DescriptorRecordDto> listRecords(@Param("params") DescriptorRecordsParams searchParams);

  long countRecords(@Param("params") DescriptorRecordsParams searchParams);

  void deleteRecords(@Param("descriptorKey") long descriptorKey);

  void createVerbatim(
      @Param("recordKey") long recordKey,
      @Param("fieldName") String fieldName,
      @Param("fieldValue") String fieldValue);

  // TODO: delete verbatim needed or cascade??

}
