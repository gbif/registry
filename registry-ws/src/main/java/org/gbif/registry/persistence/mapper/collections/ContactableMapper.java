package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Staff;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * Generic mapper to work with collections-related contacts. It works with {@link Staff} entities.
 */
public interface ContactableMapper {

  List<Staff> listContacts(@Param("key") UUID key);

  void addContact(@Param("entityKey") UUID entityKey, @Param("staffKey") UUID contactKey);

  void removeContact(@Param("entityKey") UUID entityKey, @Param("staffKey") UUID contactKey);
}
