package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.Contact;

public interface ContactMapper {

  int createContact(Contact contact);

  void updateContact(Contact contact);
}
