package org.gbif.registry.metadata.contact;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.ContactType;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * Adapt the {@link Dataset} {@link Contact} list for what metadata documents generally want.
 *
 * @author cgendreau
 */
public class ContactAdapter {

  private final Joiner JOINER = Joiner.on(" ").skipNulls();

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
   * Get ResourceCreator name as "FirstName LastName".
   *
   * @return ResourceCreator name or "" if no ResourceCreator were found
   */
  public String getResourceCreatorName() {
    Contact contact = getResourceCreator();
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
