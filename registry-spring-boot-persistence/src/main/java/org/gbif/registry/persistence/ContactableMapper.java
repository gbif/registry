package org.gbif.registry.persistence;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.Person;

import java.util.List;
import java.util.UUID;

/**
 * Generic mapper to work with collections-related contacts. It works with {@link Person} entities.
 */
// TODO: 29/08/2019 conflicts with existing one
public interface ContactableMapper {

  List<Person> listContacts(@Param("key") UUID key);

  void addContact(@Param("entityKey") UUID entityKey, @Param("personKey") UUID contactKey);

  void removeContact(@Param("entityKey") UUID entityKey, @Param("personKey") UUID contactKey);
}