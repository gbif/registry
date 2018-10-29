package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface InstitutionMapper extends TaggableMapper, IdentifiableMapper {

  Institution get(@Param("key") UUID key);

  void create(Institution institution);

  void delete(@Param("key") UUID key);

  void update(Institution institution);

  List<Institution> list(@Nullable @Param("page") Pageable page);

  List<Institution> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  long count();

  long countWithFilter(@Nullable @Param("query") String query);

  List<Staff> listContacts(@Param("key") UUID key);

  void addContact(@Param("institutionKey") UUID institutionKey, @Param("staffKey") UUID contactKey);

  void removeContact(@Param("institutionKey") UUID institutionKey, @Param("staffKey") UUID contactKey);

}
