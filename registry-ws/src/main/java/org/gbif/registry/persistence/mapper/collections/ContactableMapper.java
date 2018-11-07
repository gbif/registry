package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Person;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * Generic mapper to work with collections-related contacts. It works with {@link Person} entities.
 */
public interface ContactableMapper {

  List<Person> listContacts(@Param("key") UUID key);

  void addContact(@Param("entityKey") UUID entityKey, @Param("personKey") UUID contactKey);

  void removeContact(@Param("entityKey") UUID entityKey, @Param("personKey") UUID contactKey);
}
