package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface StaffMapper {

  Staff get(@Param("key") UUID key);

  void create(Staff staff);

  void delete(@Param("key") UUID key);

  void update(Staff staff);

  List<Staff> list(@Nullable @Param("page") Pageable page);

  List<Staff> listStaffByInstitution(
    @Param("institutionKey") UUID institutionKey, @Nullable @Param("page") Pageable page);

  List<Staff> listStaffByCollection(
    @Param("collectionKey") UUID collectionKey, @Nullable @Param("page") Pageable page);

  List<Staff> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  int count();

  int countWithFilter(@Nullable @Param("query") String query);

  int countByInstitution(@Param("institutionKey") UUID institutionKey);

  int countByCollection(@Param("collectionKey") UUID collectionKey);

}
