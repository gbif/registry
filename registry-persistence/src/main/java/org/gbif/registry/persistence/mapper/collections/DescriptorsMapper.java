package org.gbif.registry.persistence.mapper.collections;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.Record;
import org.gbif.api.model.collections.descriptors.VerbatimField;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorRecordsParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsVerbatimFieldsParams;
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

  Record getRecord(@Param("key") long key);

  void createRecord(Record entity);

  void deleteRecord(@Param("key") long key);

  void updateRecord(Record entity);

  List<Record> listRecords(@Param("params") DescriptorRecordsParams searchParams);

  long countRecords(@Param("params") DescriptorRecordsParams searchParams);

  void deleteRecords(@Param("descriptorKey") long descriptorKey);

  void createVerbatim(VerbatimField entity);

  // TODO: delete verbatim needed or cascade??

  List<VerbatimField> listVerbatims(@Param("params") DescriptorsVerbatimFieldsParams searchParams);

  long countVerbatims(@Param("params") DescriptorsVerbatimFieldsParams searchParams);
}
