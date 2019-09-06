package org.gbif.registry.metadata.contact;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.ContactType;

import java.util.List;

/**
 *
 * Adapt the {@link Dataset} {@link Contact} list for what metadata documents generally want.
 *
 * @author cgendreau
 */
public class ContactAdapter {

  private static final Joiner JOINER = Joiner.on(" ").skipNulls();

  private List<Contact> contactList;

  public ContactAdapter(List<Contact> contacts) {
    this.contactList = contacts;
  }

  /**
   * Get the list of AssociatedParties.
   * This is defined as all non-primary {@link Contact}, excluding contacts with types the following types
   * considered primary types: Originator, MetadataAuthor and AdministrativePointOfContact.
   *
   * @return list of AssociatedParties or empty list if none found
   */
  public List<Contact> getAssociatedParties() {
    List<Contact> contacts = Lists.newArrayList();
    for (Contact c : this.contactList) {
      if (!c.isPrimary() && !isPreferredContactType(c.getType())) {
        contacts.add(c);
      }
    }
    return contacts;
  }

  /**
   * @return true if contact type is considered a preferred type, or false otherwise
   */
  private boolean isPreferredContactType(ContactType type) {
    return (type != null &&
        (type == ContactType.ORIGINATOR
            || type == ContactType.ADMINISTRATIVE_POINT_OF_CONTACT
            || type == ContactType.METADATA_AUTHOR));
  }

  /**
   * Get the ResourceCreator {@link Contact}.
   * This is defined as the first primary {@link Contact} of type ContactType.ORIGINATOR.
   *
   * @return first preferred ResourceCreator found or null if none were found
   */
  public Contact getResourceCreator() {
    return getFirstPreferredType(ContactType.ORIGINATOR);
  }

  /**
   * Format the name of the contact as "FirstName LastName".
   *
   * @param contact
   * @return formatted name or "" if the contact is null or empty
   */
  public static String formatContactName(Contact contact){
    if(contact == null){
      return "";
    }
    return JOINER.join(contact.getFirstName(), contact.getLastName()).trim();
  }

  /**
   * Get the AdministrativeContact {@link Contact}.
   * This is defined as the first primary {@link Contact} of type ContactType.ADMINISTRATIVE_POINT_OF_CONTACT.
   *
   * @return first preferred AdministrativeContact found or null if none were found
   */
  public Contact getAdministrativeContact() {
    return getFirstPreferredType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
  }

  /**
   * Filter contacts based on the provided contact types.
   * The order in which the ContactType are provided will be respected in the response except missing
   * ContactType will be ommited.
   * Filtering is done by the {@link #getFirstPreferredType} method.

   *
   * @param types
   * @return filtered contacts or an empty list if none matched
   */
  public List<Contact> getFilteredContacts(ContactType ... types){
    List<Contact> contacts = Lists.newArrayList();
    Contact contact;
    for(ContactType type : types){
      contact = getFirstPreferredType(type);
      if(contact != null){
        contacts.add(contact);
      }
    }
    return contacts;
  }

  /**
   * Get the first primary {@link Contact} for the provided type.
   *
   * @return first preferred type contact found or null if nothing were found
   */
  public Contact getFirstPreferredType(ContactType type) {
    Contact pref = null;
    for (Contact c : contactList) {
      if (type == c.getType()) {
        if (pref == null || c.isPrimary()) {
          pref = c;
        }
      }
    }
    return pref;
  }

  /**
   * Get the list of {@link Contact} of type ContactType.ORIGINATOR.
   *
   * @return all creators found or empty list if none were found
   */
  public List<Contact> getCreators() {
    return getAllType(ContactType.ORIGINATOR);
  }

  /**
   * Get the list of {@link Contact} of type ContactType.ADMINISTRATIVE_POINT_OF_CONTACT.
   *
   * @return all contacts found or empty list if none were found
   */
  public List<Contact> getContacts() {
    return getAllType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
  }

  /**
   * Get the list of {@link Contact} of type ContactType.METADATA_AUTHOR.
   *
   * @return all metadataProviders found or empty list if none were found
   */
  public List<Contact> getMetadataProviders() {
    return getAllType(ContactType.METADATA_AUTHOR);
  }

  /**
   * Get all {@link Contact} for the provided type.
   *
   * @return all {@link Contact} for specified type or empty list if none found
   */
  public List<Contact> getAllType(ContactType type) {
    List<Contact> primary = Lists.newArrayList();
    for (Contact c : contactList) {
      if (type == c.getType()) {
        primary.add(c);
      }
    }
    return primary;
  }
}
