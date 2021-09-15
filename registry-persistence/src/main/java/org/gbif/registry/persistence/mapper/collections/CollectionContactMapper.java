package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Contact;

import org.springframework.stereotype.Repository;

@Repository
public interface CollectionContactMapper {

  void createContact(Contact contact);

  void updateContact(Contact contact);
}
