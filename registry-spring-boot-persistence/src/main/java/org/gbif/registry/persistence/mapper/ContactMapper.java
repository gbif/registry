package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.Contact;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMapper {

  int createContact(Contact contact);

  void updateContact(Contact contact);
}
