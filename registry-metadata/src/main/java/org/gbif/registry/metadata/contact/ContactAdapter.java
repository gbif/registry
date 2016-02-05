package org.gbif.registry.metadata.contact;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.ContactType;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

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
   * This is defined as all non-primary {@link Contact}.
   *
   * @return first preferred ResourceCreator found or null if none were found
   */
  public List<Contact> getAssociatedParties() {
    List<Contact> contacts = Lists.newArrayList();
    for (Contact c : this.contactList) {
      if (!c.isPrimary()) {
        contacts.add(c);
      }
    }
    return contacts;
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

}
