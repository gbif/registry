package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Person;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;

import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;
import static org.gbif.registry.collections.sync.diff.Utils.encodeIRN;
import static org.gbif.registry.collections.sync.diff.Utils.isIHOutdated;
import static org.gbif.registry.collections.sync.diff.Utils.mapByIrn;

class StaffDiffFinder {

  private static final String EMPTY = "";
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  private final EntityConverter entityConverter;
  private final List<Person> allGrSciCollPersons;
  private final Map<String, Set<Person>> grSciCollPersonsByIrn;

  @Builder
  private StaffDiffFinder(EntityConverter entityConverter, List<Person> allGrSciCollPersons) {
    this.entityConverter = entityConverter;
    this.allGrSciCollPersons = allGrSciCollPersons;
    this.grSciCollPersonsByIrn = mapByIrn(allGrSciCollPersons);
  }

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

  private static final Function<IHStaff, String> CONCAT_IH_FIRST_NAME =
      s -> {
        StringBuilder firstNameBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(s.getFirstName())) {
          firstNameBuilder.append(s.getFirstName());
        }
        if (!Strings.isNullOrEmpty(s.getMiddleName())) {
          firstNameBuilder.append(s.getMiddleName());
        }

        String firstName = firstNameBuilder.toString();

        if (Strings.isNullOrEmpty(firstName)) {
          return null;
        }

        return firstName.trim();
      };

  private static final Function<Person, String> CONCAT_PERSON_NAME =
      p -> {
        StringBuilder fullNameBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(p.getFirstName())) {
          fullNameBuilder.append(p.getFirstName());
        }
        if (!Strings.isNullOrEmpty(p.getLastName())) {
          fullNameBuilder.append(" ").append(p.getLastName());
        }

        String fullName = fullNameBuilder.toString();

        if (Strings.isNullOrEmpty(fullName)) {
          return null;
        }

        return WHITESPACE.matcher(fullName).replaceAll(EMPTY);
      };

  public <T extends CollectionEntity> StaffDiffResult<T> syncStaff(
      T entity, List<IHStaff> ihStaffList, List<Person> contacts) {

    StaffDiffResult.StaffDiffResultBuilder<T> diffResult =
        StaffDiffResult.<T>builder().entity(entity);

    List<Person> contactsCopy = contacts != null ? new ArrayList<>(contacts) : Collections.emptyList();
    for (IHStaff ihStaff : ihStaffList) {
      // try to find a match in the GrSciColl contacts
      Set<Person> matches = matchWithContacts(ihStaff, contactsCopy);

      if (matches.isEmpty()) {
        // no match among the contacts. We check now in all the GrSciColl persons.
        Set<Person> globalMatches = matchGlobally(ihStaff, allGrSciCollPersons);
        if (globalMatches.isEmpty()) {
          // we create a new person and add it to the entity
          Person person = entityConverter.convertToPerson(ihStaff);
          diffResult.personToCreate(person);
        } else if (globalMatches.size() > 1) {
          // conflict
          diffResult.conflict(new DiffResult.Conflict<>(ihStaff, new ArrayList<>(globalMatches)));
        } else {
          // there is one match.
          Person globalMatch = globalMatches.iterator().next();
          compareStaff(ihStaff, globalMatch, diffResult);
        }
      } else if (matches.size() > 1) {
        // conflict
        contactsCopy.removeAll(matches);
        diffResult.conflict(new DiffResult.Conflict<>(ihStaff, new ArrayList<>(matches)));
      } else {
        // there is one match
        Person existing = matches.iterator().next();
        contactsCopy.remove(existing);
        compareStaff(ihStaff, existing, diffResult);
      }
    }

    // remove from the GrSciColl entity the persons that don't exist in IH
    diffResult.personsToRemoveFromEntity(contactsCopy);

    return diffResult.build();
  }

  private <T extends CollectionEntity> void compareStaff(
      IHStaff ihStaff, Person existing, StaffDiffResult.StaffDiffResultBuilder<T> diffResult) {

    if (isIHOutdated(ihStaff, existing)) {
      // add issue
      diffResult.outdatedStaff(new DiffResult.IHOutdated<>(ihStaff, existing));
      return;
    }

    Person person = entityConverter.convertToPerson(ihStaff, existing);
    if (!person.lenientEquals(existing)) {
      diffResult.personToUpdate(new DiffResult.PersonDiffResult(existing, person));
    } else {
      diffResult.personNoChange(person);
    }
  }

  private Set<Person> matchGlobally(IHStaff ihStaff, List<Person> grSciCollPersons) {
    // first try with IRNs
    Set<Person> matchesWithIrn =
        grSciCollPersonsByIrn.getOrDefault(encodeIRN(ihStaff.getIrn()), Collections.emptySet());

    if (!matchesWithIrn.isEmpty()) {
      return matchesWithIrn;
    }

    // we try to match with fields
    return matchWithFields(ihStaff, grSciCollPersons, 11);
  }

  private Set<Person> matchWithContacts(IHStaff ihStaff, List<Person> grSciCollPersons) {
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
      return irnMatches;
    }

    // no irn matches, we try to match with the fields
    return matchWithFields(ihStaff, grSciCollPersons, 10);
  }

  @VisibleForTesting
  Set<Person> matchWithFields(IHStaff ihStaff, List<Person> persons, int minimumScore) {
    if (persons.isEmpty()) {
      return Collections.emptySet();
    }

    StaffNormalized ihStaffNorm = buildIHStaffNormalized(ihStaff, entityConverter);

    int maxScore = 0;
    Set<Person> bestMatches = new HashSet<>();
    for (Person person : persons) {
      StaffNormalized personNorm = buildGrSciCollPersonNormalized(person);
      int equalityScore = getEqualityScore(ihStaffNorm, personNorm);

      if (equalityScore < minimumScore) {
        continue;
      }

      if (equalityScore > maxScore) {
        bestMatches.clear();
        bestMatches.add(person);
        maxScore = equalityScore;
      } else if (equalityScore > 0 && equalityScore == maxScore) {
        bestMatches.add(person);
      }
    }

    return bestMatches;
  }

  private static StaffNormalized buildIHStaffNormalized(
      IHStaff ihStaff, EntityConverter entityConverter) {
    StaffNormalized.StaffNormalizedBuilder ihBuilder =
        StaffNormalized.builder()
            .fullName(CONCAT_IH_NAME.apply(ihStaff))
            .firstName(CONCAT_IH_FIRST_NAME.apply(ihStaff))
            .lastName(ihStaff.getLastName())
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
          .country(entityConverter.matchCountry(ihStaff.getAddress().getCountry()));
    }

    return ihBuilder.build();
  }

  private static StaffNormalized buildGrSciCollPersonNormalized(Person person) {
    StaffNormalized.StaffNormalizedBuilder personBuilder =
        StaffNormalized.builder()
            .fullName(CONCAT_PERSON_NAME.apply(person))
            .firstName(person.getFirstName())
            .lastName(person.getLastName())
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
          .country(person.getMailingAddress().getCountry());
    }

    return personBuilder.build();
  }

  private static int getEqualityScore(StaffNormalized staff1, StaffNormalized staff2) {
    BiPredicate<String, String> compareStrings =
        (s1, s2) -> {
          if (!Strings.isNullOrEmpty(s1) && !Strings.isNullOrEmpty(s2)) {
            return s1.equalsIgnoreCase(s2);
          }
          return false;
        };

    BiPredicate<String, String> compareNamePartially =
        (s1, s2) -> {
          if (!Strings.isNullOrEmpty(s1) && !Strings.isNullOrEmpty(s2)) {
            return s1.startsWith(s2) || s2.startsWith(s1);
          }
          return false;
        };

    int score = 0;
    if (compareStrings.test(staff1.email, staff2.email)) {
      score += 10;
    }

    if (compareStrings.test(staff1.fullName, staff2.fullName)) {
      score += 10;
    } else {
      if (compareStrings.test(staff1.firstName, staff2.firstName)) {
        score += 5;
      } else if (compareNamePartially.test(staff1.firstName, staff2.firstName)) {
        score += 4;
      }

      if (compareStrings.test(staff1.lastName, staff2.lastName)) {
        score += 5;
      } else if (compareNamePartially.test(staff1.lastName, staff2.lastName)) {
        score += 4;
      }
    }

    // at least the name or the email should match
    if (score == 0) {
      return score;
    }

    if (compareStrings.test(staff1.phone, staff2.phone)) {
      score += 3;
    }
    if (staff2.country != null && staff1.country == staff2.country) {
      score += 3;
    }
    if (compareStrings.test(staff1.city, staff2.city)) {
      score += 2;
    }
    if (compareStrings.test(staff1.position, staff2.position)) {
      score += 2;
    }
    if (compareStrings.test(staff1.fax, staff2.fax)) {
      score += 1;
    }
    if (compareStrings.test(staff1.street, staff2.street)) {
      score += 1;
    }
    if (compareStrings.test(staff1.state, staff2.state)) {
      score += 1;
    }
    if (compareStrings.test(staff1.zipCode, staff2.zipCode)) {
      score += 1;
    }

    return score;
  }

  /** Contains all the common field between IH staff and GrSciColl persons. */
  @Data
  @Builder
  private static class StaffNormalized {
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String fax;
    private String position;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private Country country;
  }
}
