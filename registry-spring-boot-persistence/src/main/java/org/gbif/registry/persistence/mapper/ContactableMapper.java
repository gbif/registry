package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.ContactType;

import java.util.List;
import java.util.UUID;

public interface ContactableMapper {

  int addContact(
      @Param("targetEntityKey") UUID entityKey,
      @Param("contactKey") int contactKey,
      @Param("type") ContactType contactType,
      @Param("isPrimary") boolean isPrimary
  );

  /**
   * Updates the current primary contact of the given type if it exists and makes it non primary.
   */
  void updatePrimaryContacts(@Param("targetEntityKey") UUID entityKey, @Param("type") ContactType contactType);

  /**
   * Updates the contact if it exists, updating the type and is_primary columns.
   */
  void updateContact(@Param("targetEntityKey") UUID entityKey, @Param("contactKey") Integer contactKey,
                     @Param("type") ContactType contactType, @Param("primary") boolean primary);

  int deleteContact(@Param("targetEntityKey") UUID entityKey, @Param("contactKey") int contactKey);

  /**
   * Delete all contacts associated with the given entity
   */
  int deleteContacts(@Param("targetEntityKey") UUID entityKey);

  List<Contact> listContacts(@Param("targetEntityKey") UUID targetEntityKey);

  /**
   * Checks if the contact is associated with the entity.
   *
   * @return true only if the contact and entity are related
   */
  Boolean areRelated(@Param("targetEntityKey") UUID targetEntityKey, @Param("contactKey") int contactKey);
}

