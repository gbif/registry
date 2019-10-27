package org.gbif.registry.metadata;

import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class tha generates a Citation String from {@link Dataset} and {@link Organization} objects.
 * Documentation : /docs/citations.md
 */
public class CitationGenerator {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final ContactType MANDATORY_CONTACT_TYPE = ContactType.ORIGINATOR;
  private static final EnumSet<ContactType> AUTHOR_CONTACT_TYPE = EnumSet.of(ContactType.ORIGINATOR,
    ContactType.METADATA_AUTHOR);
  private static final Predicate<Contact> IS_NAME_PROVIDED_FCT = ctc -> StringUtils.isNotBlank(ctc.getFirstName()) &&
    StringUtils.isNotBlank(ctc.getLastName());
  private static final Predicate<Contact> IS_ELIGIBLE_CONTACT_TYPE = ctc -> ctc.getType() != null && AUTHOR_CONTACT_TYPE.contains(ctc.getType());

  /**
   * Utility class
   */
  private CitationGenerator() {
  }

  public static String generateCitation(Dataset dataset, Organization org) {
    Objects.requireNonNull(org, "Organization shall be provided");
    return generateCitation(dataset, org.getTitle());
  }

  /**
   * Generate a citation for a {@link Dataset} and its {@link Organization}.
   * TODO add support for i18n
   *
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

    boolean authorsNameAvailable = StringUtils.isNotBlank(authors);
    authors = authorsNameAvailable ? authors : organizationTitle;

    //only add a dot if we are not gonna add it with the year
    authors += dataset.getPubDate() == null ? "." : "";
    joiner.add(authors);

    if (dataset.getPubDate() != null) {
      joiner.add("(" + dataset.getPubDate().toInstant().atZone(UTC).getYear() + ").");
    }

    // add title
    joiner.add(StringUtils.trim(dataset.getTitle()) + ".");

    // add version
    if (dataset.getVersion() != null) {
      joiner.add("Version " + dataset.getVersion() + ".");
    }

    // add publisher except if it was used instead of the authors
    if (authorsNameAvailable) {
      joiner.add(StringUtils.trim(organizationTitle) + ".");
    }

    if (dataset.getType() != null) {
      joiner.add(StringUtils.capitalize(dataset.getType().name().replace('_', ' ').toLowerCase()));
    }
    joiner.add("dataset");

    // add DOI as the identifier.
    if (dataset.getDoi() != null) {
      try {
        joiner.add(URLDecoder.decode(dataset.getDoi().getUrl().toString(), StandardCharsets.UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalArgumentException("Couldn't decode DOI URL", e);
      }
    } else {
      //??
    }

    joiner.add("accessed via GBIF.org on " + LocalDate.now(UTC) + ".");

    return joiner.toString();
  }

  /**
   * Extracts an ordered list of unique authors from a list of contacts.
   * A {@link Contact} is identified as an author when his {@link ContactType} is contained in
   * {@link #AUTHOR_CONTACT_TYPE}.
   * But, we shall at least have one contact of type MANDATORY_CONTACT_TYPE.
   *
   * @param contacts list of contacts available
   * @return ordered list of authors or empty list, never null
   */
  public static List<Contact> getAuthors(List<Contact> contacts) {
    if (contacts == null || contacts.isEmpty()) {
      return Collections.emptyList();
    }

    List<Contact> uniqueContacts = getUniqueAuthors(contacts,
      (ctc) -> IS_NAME_PROVIDED_FCT.and(IS_ELIGIBLE_CONTACT_TYPE).test(ctc));

    // make sure we have at least one instance of {@link #MANDATORY_CONTACT_TYPE}
    Optional<Contact> firstOriginator = uniqueContacts.stream()
      .filter(ctc -> MANDATORY_CONTACT_TYPE == ctc.getType())
      .findFirst();

    if (firstOriginator.isPresent()) {
      return uniqueContacts;
    }
    return Collections.emptyList();
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
      return Collections.emptyList();
    }

    return authors.stream()
      .filter(IS_NAME_PROVIDED_FCT)
      .map(CitationGenerator::getAuthorName)
      .collect(Collectors.toList());
  }

  /**
   * This method is used to get the list of "unique" authors.
   * Currently, uniqueness is based on lastName + firstNames.
   * The order of the provided list will be preserved which also means the first {@link ContactType} found for
   * a contact is the one that will be used for this contact (after applying the filter).
   *
   * @param authors a list of contacts representing possible authors
   * @param filter  {@link Predicate} used to pre-filter contacts
   * @return
   */
  private static List<Contact> getUniqueAuthors(List<Contact> authors, Predicate<Contact> filter) {
    List<Contact> uniqueContact = new ArrayList<>();
    if (authors != null) {
      authors.forEach(ctc -> {
        if (filter.test(ctc) && isNotAlreadyInList(ctc, uniqueContact)) {
          uniqueContact.add(ctc);
        }
      });
    }
    return uniqueContact;
  }

  /**
   * Check if a specific {@link Contact} is NOT already in the list of "unique" contact.
   * Currently, uniqueness is based on the comparisons of lastName and firstNames.
   *
   * @param ctc
   * @param uniqueContact
   * @return
   */
  private static boolean isNotAlreadyInList(final Contact ctc, List<Contact> uniqueContact) {
    return uniqueContact.stream()
      .noneMatch(contact ->
        StringUtils.equalsIgnoreCase(ctc.getLastName(), contact.getLastName())
          && StringUtils.equalsIgnoreCase(ctc.getFirstName(), contact.getFirstName()));
  }

  /**
   * Given a {@link Contact}, generates a a String for that contact for citation purpose.
   * The organization will be used (if present) in case we don't have both lastName and firstNames of the contact.
   *
   * @param creator
   * @return
   */
  public static String getAuthorName(Contact creator) {
    StringBuilder sb = new StringBuilder();
    String lastName = StringUtils.trimToNull(creator.getLastName());
    String firstNames = StringUtils.trimToNull(creator.getFirstName());
    String organization = StringUtils.trimToNull(creator.getOrganization());

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
    } else if (organization != null) {
      sb.append(organization);
    }
    return sb.toString();
  }
}
