package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface StaffMapper extends CrudMapper<Staff> {

  List<Staff> listStaffByInstitution(
    @Param("institutionKey") UUID institutionKey, @Nullable @Param("page") Pageable page);

  List<Staff> listStaffByCollection(
    @Param("collectionKey") UUID collectionKey, @Nullable @Param("page") Pageable page);

  long countByInstitution(@Param("institutionKey") UUID institutionKey);

  long countByCollection(@Param("collectionKey") UUID collectionKey);

}
