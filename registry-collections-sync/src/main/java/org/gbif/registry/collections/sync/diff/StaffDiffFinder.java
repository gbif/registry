package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.ih.IHStaff;
import org.gbif.registry.collections.sync.notification.IssueFactory;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;
import static org.gbif.registry.collections.sync.diff.Utils.encodeIRN;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class StaffDiffFinder {

  private static final String EMPTY = "";
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  private static final Function<IHStaff, String> CONCAT_IH_NAME =
      s -> {
        StringBuilder fullNameBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(s.getFirstName())) {
          fullNameBuilder.append(s.getFirstName());
        }
        if (!Strings.isNullOrEmpty(s.getMiddleName())) {
          fullNameBuilder.append(s.getMiddleName());
        }
        if (!Strings.isNullOrEmpty(s.getLastName())) {
          fullNameBuilder.append(s.getLastName());
        }

        String fullName = fullNameBuilder.toString();

        if (Strings.isNullOrEmpty(fullName)) {
          return null;
        }

        return WHITESPACE.matcher(fullName).replaceAll(EMPTY);
      };

  private static final Function<Person, String> CONCAT_PERSON_NAME =
      p -> {
        StringBuilder fullNameBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(p.getFirstName())) {
          fullNameBuilder.append(p.getFirstName());
        }
        if (!Strings.isNullOrEmpty(p.getLastName())) {
          fullNameBuilder.append(p.getLastName());
        }

        String fullName = fullNameBuilder.toString();

        if (Strings.isNullOrEmpty(fullName)) {
          return null;
        }

        return WHITESPACE.matcher(fullName).replaceAll(EMPTY);
      };

  public static StaffDiffResult syncStaff(
      List<IHStaff> ihStaffList,
      List<Person> grSciCollContacts,
      EntityConverter entityConverter,
      IssueFactory issueFactory) {

    StaffDiffResult.StaffDiffResultBuilder diffResult = StaffDiffResult.builder();

    // copy the persons to another list to be able to remove the ones that have a match
    List<Person> personsCopy =
        grSciCollContacts != null ? new ArrayList<>(grSciCollContacts) : new ArrayList<>();

    for (IHStaff ihStaff : ihStaffList) {
      // try to find a match in the GrSciColl contacts
      Optional<Set<Person>> matchesOpt = match(ihStaff, personsCopy);

      if (!matchesOpt.isPresent()) {
        // if there is no match we create a new person
        Person person = entityConverter.convertToPerson(ihStaff);
        diffResult.personToCreate(person);
        continue;
      }

      Set<Person> matches = matchesOpt.get();
      if (matches.size() > 1) {
        // conflict
        personsCopy.removeAll(matches);
        diffResult.conflict(issueFactory.createMultipleStaffIssue(matches, ihStaff));
      } else if (matches.size() == 1) {
        Person existing = matches.iterator().next();
        personsCopy.remove(existing);

        if (Utils.isIHOutdated(ihStaff.getDateModified(), existing)) {
          // add issue
          diffResult.conflict(issueFactory.createOutdatedIHStaffIssue(existing, ihStaff));
          continue;
        }

        Person person = entityConverter.convertToPerson(ihStaff, existing);
        if (!person.lenientEquals(existing)) {
          diffResult.personToUpdate(new DiffResult.PersonDiffResult(existing, person));
        } else {
          diffResult.personNoChange(person);
        }
      }
    }

    // remove the GrSciColl persons that don't exist in IH
    personsCopy.forEach(diffResult::personToDelete);

    return diffResult.build();
  }

  private static Optional<Set<Person>> match(IHStaff ihStaff, List<Person> grSciCollPersons) {
    // try to find a match by using the IRN identifiers
    String irn = encodeIRN(ihStaff.getIrn());
    Set<Person> irnMatches =
        grSciCollPersons.stream()
            .filter(
                p ->
                    p.getIdentifiers().stream()
                        .anyMatch(i -> Objects.equals(irn, i.getIdentifier())))
            .collect(Collectors.toSet());

    if (!irnMatches.isEmpty()) {
      return Optional.of(irnMatches);
    }

    // no irn matches, we try to match with the fields
    return matchWithFields(ihStaff, grSciCollPersons);
  }

  private static Optional<Set<Person>> matchWithFields(IHStaff ihStaff, List<Person> persons) {
    if (persons.isEmpty()) {
      return Optional.empty();
    }

    StaffNormalized ihStaffNorm = buildIHStaffNormalized(ihStaff);

    int maxScore = 0;
    Set<Person> bestMatches = new HashSet<>();
    for (Person person : persons) {
      StaffNormalized personNorm = buildGrSciCollPersonNormalized(person);
      int equalityScore = getEqualityScore(ihStaffNorm, personNorm);
      if (equalityScore > maxScore) {
        bestMatches.clear();
        bestMatches.add(person);
        maxScore = equalityScore;
      } else if (equalityScore > 0 && equalityScore == maxScore) {
        bestMatches.add(person);
      }
    }

    return bestMatches.isEmpty() ? Optional.empty() : Optional.of(bestMatches);
  }

  private static StaffNormalized buildIHStaffNormalized(IHStaff ihStaff) {
    StaffNormalized.StaffNormalizedBuilder ihBuilder =
        StaffNormalized.builder()
            .fullName(CONCAT_IH_NAME.apply(ihStaff))
            .position(ihStaff.getPosition());

    if (ihStaff.getContact() != null) {
      ihBuilder
          .email(ihStaff.getContact().getEmail())
          .phone(ihStaff.getContact().getPhone())
          .fax(ihStaff.getContact().getFax());
    }

    if (ihStaff.getAddress() != null) {
      ihBuilder
          .street(ihStaff.getAddress().getStreet())
          .city(ihStaff.getAddress().getCity())
          .state(ihStaff.getAddress().getState())
          .zipCode(ihStaff.getAddress().getZipCode())
          .country(ihStaff.getAddress().getCountry());
    }

    return ihBuilder.build();
  }

  private static StaffNormalized buildGrSciCollPersonNormalized(Person person) {
    StaffNormalized.StaffNormalizedBuilder personBuilder =
        StaffNormalized.builder()
            .fullName(CONCAT_PERSON_NAME.apply(person))
            .position(person.getPosition())
            .email(person.getEmail())
            .phone(person.getPhone())
            .fax(person.getFax());

    if (person.getMailingAddress() != null) {
      personBuilder
          .street(person.getMailingAddress().getAddress())
          .city(person.getMailingAddress().getCity())
          .state(person.getMailingAddress().getProvince())
          .zipCode(person.getMailingAddress().getPostalCode())
          .country(person.getMailingAddress().getPostalCode());
    }

    return personBuilder.build();
  }

  private static int getEqualityScore(StaffNormalized s1, StaffNormalized s2) {
    int score = 0;
    if (Objects.equals(s1.fullName, s2.fullName)) {
      score += 4;
    }
    if (Objects.equals(s1.email, s2.email)) {
      score += 4;
    }

    // at least the name or the email should match
    if (score == 0) {
      return score;
    }

    if (Objects.equals(s1.phone, s2.phone)) {
      score += 3;
    }
    if (Objects.equals(s1.country, s2.country)) {
      score += 3;
    }
    if (Objects.equals(s1.city, s2.city)) {
      score += 2;
    }
    if (Objects.equals(s1.position, s2.position)) {
      score += 2;
    }
    if (Objects.equals(s1.fax, s2.fax)) {
      score += 1;
    }
    if (Objects.equals(s1.street, s2.street)) {
      score += 1;
    }
    if (Objects.equals(s1.state, s2.state)) {
      score += 1;
    }
    if (Objects.equals(s1.zipCode, s2.zipCode)) {
      score += 1;
    }

    return score;
  }

  /** Contains all the common field between IH staff and GrSciColl persons. */
  @Data
  @Builder
  private static class StaffNormalized {
    private String fullName;
    private String email;
    private String phone;
    private String fax;
    private String position;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
  }
}
