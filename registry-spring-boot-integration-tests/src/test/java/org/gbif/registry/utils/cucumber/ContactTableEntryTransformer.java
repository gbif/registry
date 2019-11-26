package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ContactTableEntryTransformer implements TableEntryTransformer<Contact> {

  @Override
  public Contact transform(Map<String, String> entry) {
    Contact contact = new Contact();
    Optional.ofNullable(entry.get("key"))
      .map(Integer::parseInt)
      .ifPresent(contact::setKey);
    Optional.ofNullable(entry.get("primary"))
      .map(Boolean::parseBoolean)
      .ifPresent(contact::setPrimary);
    Optional.ofNullable(entry.get("position"))
      .map(Collections::singletonList)
      .ifPresent(contact::setPosition);
    contact.setDescription(entry.get("description"));
    contact.setOrganization(entry.get("organization"));
    contact.setCity(entry.get("city"));
    contact.setProvince(entry.get("province"));
    Optional.ofNullable(entry.get("country"))
      .map(Country::valueOf)
      .ifPresent(contact::setCountry);
    contact.setPostalCode(entry.get("postalCode"));
    contact.setCreatedBy(entry.get("createdBy"));
    contact.setModifiedBy(entry.get("modifiedBy"));
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
