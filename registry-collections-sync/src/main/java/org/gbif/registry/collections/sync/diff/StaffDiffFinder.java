package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.ih.IHStaff;
import org.gbif.registry.collections.sync.notification.Issue;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;
import static org.gbif.registry.collections.sync.ih.IHUtils.encodeIRN;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class StaffDiffFinder {

  private static final String EMPTY = "";
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  private static final Function<IHStaff, String> CONCAT_IH_NAME =
      s ->
          WHITESPACE
              .matcher(s.getFirstName() + s.getMiddleName() + s.getLastName())
              .replaceAll(EMPTY);

  private static final Function<Person, String> CONCAT_PERSON_NAME =
      p -> WHITESPACE.matcher(p.getFirstName() + p.getLastName()).replaceAll(EMPTY);

  public static StaffDiffResult syncStaff(List<IHStaff> ihStaffList, Contactable entity) {

    StaffDiffResult.Builder syncResult = new StaffDiffResult.Builder();

    // copy the persons to another list to be able to remove the ones that have a match
    List<Person> personsCopy =
        entity.getContacts() != null ? new ArrayList<>(entity.getContacts()) : new ArrayList<>();

    for (IHStaff ihStaff : ihStaffList) {
      // try to find a match in the GrSciColl contacts
      Optional<List<Person>> matchesOpt = match(ihStaff, personsCopy);

      if (!matchesOpt.isPresent()) {
        // if there is no match we create a new person
        Person person = EntityConverter.createPerson().fromIHStaff(ihStaff).convert();
        syncResult.personToCreate(person);
        continue;
      }

      List<Person> matches = matchesOpt.get();
      if (matches.size() > 1) {
        // conflict
        personsCopy.removeAll(matches);
        syncResult.conflict(Issue.createMultipleStaffIssue(matches, ihStaff));
      } else if (matches.size() == 1) {
        Person existing = matches.get(0);
        Person person =
            EntityConverter.createPerson().fromIHStaff(ihStaff).withExisting(existing).convert();
        if (!person.lenientEquals(existing)) {
          syncResult.personToUpdate(new DiffResult.PersonDiffResult(existing, person));
        } else {
          syncResult.personNoChange(person);
        }
      }
    }

    // remove the GrSciColl persons that don't exist in IH
    personsCopy.forEach(syncResult::personToDelete);

    return syncResult.build();
  }

  private static Optional<List<Person>> match(IHStaff ihStaff, List<Person> grSciCollPersons) {
    // try to find a match by using the IRN identifiers
    String irn = encodeIRN(ihStaff.getIrn());
    List<Person> irnMatches =
        grSciCollPersons.stream()
            .filter(
                p ->
                    p.getIdentifiers().stream()
                        .anyMatch(i -> Objects.equals(irn, i.getIdentifier())))
            .collect(Collectors.toList());

    if (!irnMatches.isEmpty()) {
      return Optional.of(irnMatches);
    }

    // no irn matches, we try to match with the fields
    return matchWithFields(ihStaff, grSciCollPersons).map(Collections::singletonList);
  }

  private static Optional<Person> matchWithFields(IHStaff ihStaff, List<Person> persons) {
    if (persons.isEmpty()) {
      return Optional.empty();
    }

    StaffNormalized ihStaffNorm = buildIHStaffNormalized(ihStaff);

    int maxScore = 0;
    Person bestMatch = null;
    for (Person person : persons) {
      StaffNormalized personNorm = buildGrSciCollPersonNormalized(person);
      if (getEqualityScore(ihStaffNorm, personNorm) > maxScore) {
        bestMatch = person;
      }
    }

    return Optional.ofNullable(bestMatch);
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

    if (s1.fullName.equals(s2.fullName)) {
      score += 4;
    }
    if (s1.email.equals(s2.email)) {
      score += 4;
    }
    if (s1.phone.equals(s2.phone)) {
      score += 3;
    }
    if (s1.country.equals(s2.country)) {
      score += 3;
    }
    if (s1.city.equals(s2.city)) {
      score += 2;
    }
    if (s1.position.equals(s2.position)) {
      score += 2;
    }
    if (s1.fax.equals(s2.fax)) {
      score += 1;
    }
    if (s1.street.equals(s2.street)) {
      score += 1;
    }
    if (s1.state.equals(s2.state)) {
      score += 1;
    }
    if (s1.zipCode.equals(s2.zipCode)) {
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
