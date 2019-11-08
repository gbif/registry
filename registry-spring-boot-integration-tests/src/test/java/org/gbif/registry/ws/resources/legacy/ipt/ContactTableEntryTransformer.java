package org.gbif.registry.ws.resources.legacy.ipt;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.ContactType;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ContactTableEntryTransformer implements TableEntryTransformer<Contact> {

  @Override
  public Contact transform(Map<String, String> entry) {
    Contact contact = new Contact();
    Optional.ofNullable(entry.get("type"))
      .map(ContactType::inferType)
      .ifPresent(contact::setType);
    Optional.ofNullable(entry.get("email"))
      .map(Collections::singletonList)
      .ifPresent(contact::setEmail);
    contact.setFirstName(entry.get("firstName"));
    contact.setLastName(entry.get("lastName"));
    Optional.ofNullable(entry.get("address"))
      .map(Collections::singletonList)
      .ifPresent(contact::setAddress);
    Optional.ofNullable(entry.get("phone"))
      .map(Collections::singletonList)
      .ifPresent(contact::setPhone);
    Optional.ofNullable(entry.get("primary"))
      .map(Boolean::parseBoolean)
      .ifPresent(contact::setPrimary);

    return contact;
  }
}
