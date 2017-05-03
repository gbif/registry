package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper class tha generates a Citation String from {@link Dataset} and {@link Organization} objects.
 */
public class CitationGenerator {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final EnumSet<ContactType> AUTHOR_CONTACT_TYPE = EnumSet.of(ContactType.ORIGINATOR,
          ContactType.METADATA_AUTHOR);

  /**
   * Utility class
   */
  private CitationGenerator(){}

  public static String generateCitation(Dataset dataset, Organization org) {
    Objects.requireNonNull(org, "Organization shall be provided");
    return generateCitation(dataset, org.getTitle());
  }

  /**
   * Generate a citation for a {@link Dataset} and its {@link Organization}.
   * TODO add support for i18n
   * @param dataset
   * @param organizationTitle
   * @return generated citation as {@link String}
   */
  public static String generateCitation(Dataset dataset, String organizationTitle) {

    Objects.requireNonNull(dataset, "Dataset shall be provided");
    Objects.requireNonNull(organizationTitle, "Organization title shall be provided");

    StringJoiner joiner = new StringJoiner(" ");
    List<String> authorsName = generateAuthorsName(getAuthors(dataset.getContacts()));
    String authors = authorsName.stream().collect(Collectors.joining(", "));

    if (StringUtils.isNotBlank(authors)) {
      //only add a dot if we are not gonna add it with the year
      authors += dataset.getPubDate() == null ? "." : "";
      joiner.add(authors);
    }

    if (dataset.getPubDate() != null) {
      joiner.add("(" + dataset.getPubDate().toInstant().atZone(UTC).getYear() + ").");
    }

    // add title
    joiner.add(StringUtils.trim(dataset.getTitle()) + ".");

    // add version
    if (dataset.getVersion() != null) {
      joiner.add("Version " + dataset.getVersion() + ".");
    }

    // add publisher
    joiner.add(StringUtils.trim(organizationTitle) + ".");

    if (dataset.getType() != null) {
      joiner.add(StringUtils.capitalize(dataset.getType().name().toLowerCase()));
    }
    joiner.add("Dataset");

    // add DOI as the identifier.
    if (dataset.getDoi() != null) {
      joiner.add(dataset.getDoi().getUrl().toString());
    } else {
      //??
    }

    joiner.add("accessed via GBIF.org on " + LocalDate.now(UTC) + ".");

    return joiner.toString();
  }

  /**
   * Extracts an ordered list of unique authors from a list of contacts.
   * A {@link Contact} is identified as an author when his {@link ContactType} is contained in
   * {@link #AUTHOR_CONTACT_TYPE}
   *
   * @param contacts list of contacts available
   *
   * @return ordered list of authors or empty list, never null
   */
  public static List<Contact> getAuthors(List<Contact> contacts) {
    if (contacts == null || contacts.isEmpty()) {
      return new ArrayList<>();
    }

    List<Contact> uniqueContacts = getUniqueAuthors(contacts);
    return uniqueContacts.stream()
            .filter(ctc -> ctc.getType() != null && AUTHOR_CONTACT_TYPE.contains(ctc.getType()))
            .collect(Collectors.toList());
  }

  /**
   * Given a list of authors, generates a {@link List} of {@link String} representing the authors name.
   * If a contact doesn't have a first AND last name it will not be included.
   *
   * @param authors ordered list of authors
   * @return list of author names (if it can be generated) or empty list, never null
   */
  public static List<String> generateAuthorsName(List<Contact> authors) {
    if (authors == null || authors.isEmpty()) {
      return new ArrayList<>();
    }

    return authors.stream()
            .filter(ctc -> StringUtils.isNotBlank(ctc.getFirstName()) && StringUtils.isNotBlank(ctc.getLastName()))
            .map(CitationGenerator::getAuthorName)
            .collect(Collectors.toList());
  }


  /**
   * This method is used to get the list of "unique" authors.
   * Currently, uniqueness is based on lastName + firstNames.
   * The order of the provided list will be preserved which also means the first {@link ContactType} found for
   * a contact is the one that will be used for this contact.
   * @param authors
   * @return
   */
  private static List<Contact> getUniqueAuthors(List<Contact> authors){
    List<Contact> uniqueContact = new ArrayList<>();
    if(authors != null) {
      authors.forEach(ctc -> {
        if (isNotAlreadyInList(ctc, uniqueContact)) {
          uniqueContact.add(ctc);
        }
      });
    }
    return uniqueContact;
  }

  /**
   * Check if a specific {@link Contact} is NOT already in the list of "unique" contact.
   * Currently, uniqueness is based on the comparisons of lastName and firstNames.
   * @param ctc
   * @param uniqueContact
   * @return
   */
  private static boolean isNotAlreadyInList(final Contact ctc, List<Contact> uniqueContact) {
    return !uniqueContact.stream()
            .filter( contact -> StringUtils.equalsIgnoreCase(ctc.getLastName(), contact.getLastName())
                    && StringUtils.equalsIgnoreCase(ctc.getFirstName(), contact.getFirstName()))
            .findFirst().isPresent();
  }

  /**
   * Given a {@link Contact}, generates a a String for that contact for citation purpose.
   * The organisation will be used (if present) in case we don't have both lastName and firstNames of the contact.
   *
   * @param creator
   *
   * @return
   */
  public static String getAuthorName(Contact creator) {
    StringBuilder sb = new StringBuilder();
    String lastName = StringUtils.trimToNull(creator.getLastName());
    String firstNames = StringUtils.trimToNull(creator.getFirstName());
    String organisation = StringUtils.trimToNull(creator.getOrganization());

    if (lastName != null && firstNames != null) {
      sb.append(lastName);
      sb.append(" ");
      // add first initial of each first name, capitalized
      String[] names = firstNames.split("\\s+");

      sb.append(Arrays.stream(names)
              .filter(str -> !StringUtils.isBlank(str))
              .map(str -> StringUtils.upperCase(String.valueOf(str.charAt(0))))
              .collect(Collectors.joining(" ")));
    } else if (lastName != null) {
      sb.append(lastName);
    } else if (organisation != null) {
      sb.append(organisation);
    }
    return sb.toString();
  }
}
