package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import org.junit.Test;

import static org.gbif.registry.collections.sync.diff.Utils.encodeIRN;

import static org.junit.Assert.*;

/** Tests the {@link StaffDiffFinder}. */
public class StaffDiffFinderTest {

  private static final EntityConverter ENTITY_CONVERTER =
      EntityConverter.builder()
          .countries(Arrays.asList("U.K.", "U.S.A.", "United Kingdom", "United States"))
          .creationUser("test-user")
          .build();
  private static final StaffDiffFinder STAFF_DIFF_FINDER =
      StaffDiffFinder.builder()
          .entityConverter(ENTITY_CONVERTER)
          .allGrSciCollPersons(Collections.emptyList())
          .build();
  private static final String IRN_TEST = "1";
  private static final Institution DEFAULT_INSTITUTION = new Institution();

  static {
    DEFAULT_INSTITUTION.setKey(UUID.randomUUID());
    DEFAULT_INSTITUTION.setCode("code");
  }

  @Test
  public void syncStaffTest() {
    TestStaff staffToUpdate = createTestStaffToUpdate();
    TestStaff staffToCreate = createTestStaffToCreate();
    TestStaff staffToDelete = createTestStaffToDelete();
    TestStaff staffNoChange = createTestStaffNoChange();
    List<TestStaff> allStaff =
        ImmutableList.of(staffToUpdate, staffToCreate, staffToDelete, staffNoChange);

    List<IHStaff> ihStaff =
        allStaff.stream()
            .filter(i -> i.getIhStaff() != null)
            .map(TestStaff::getIhStaff)
            .collect(Collectors.toList());
    List<Person> grSciCollPersons =
        allStaff.stream()
            .filter(i -> i.getPerson() != null)
            .map(TestStaff::getPerson)
            .collect(Collectors.toList());

    DiffResult.StaffDiffResult<Institution> result =
        STAFF_DIFF_FINDER.syncStaff(DEFAULT_INSTITUTION, ihStaff, grSciCollPersons);
    assertEquals(1, result.getPersonsToCreate().size());
    assertEquals(1, result.getPersonsToUpdate().size());
    assertEquals(1, result.getPersonsNoChange().size());
    assertEquals(1, result.getPersonsToRemoveFromEntity().size());

    assertEquals(staffNoChange.person, result.getPersonsNoChange().get(0));
    assertNotEquals(
        result.getPersonsToUpdate().get(0).getNewPerson(),
        result.getPersonsToUpdate().get(0).getOldPerson());
    assertEquals(staffToDelete.person, result.getPersonsToRemoveFromEntity().get(0));
    assertFalse(grSciCollPersons.contains(result.getPersonsToCreate().get(0)));
  }

  @Test
  public void outdatedIHConflictTest() {
    IHStaff outdatedStaff = new IHStaff();
    outdatedStaff.setIrn(IRN_TEST);
    outdatedStaff.setDateModified("2018-01-01");

    Person upToDatePerson = new Person();
    upToDatePerson.setKey(UUID.randomUUID());
    upToDatePerson.setModified(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    upToDatePerson.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    DiffResult.StaffDiffResult<Institution> result =
        STAFF_DIFF_FINDER.syncStaff(
            DEFAULT_INSTITUTION,
            Collections.singletonList(outdatedStaff),
            Collections.singletonList(upToDatePerson));

    assertEquals(1, result.getOutdatedStaff().size());
  }

  @Test
  public void syncWithGlobalMatchUpdateTest() {
    IHStaff s = new IHStaff();
    s.setCode("UARK");
    s.setLastName("Last");
    s.setMiddleName("M.");
    s.setFirstName("First");
    s.setPosition("Collections Manager");

    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setEmail("a@a.com");
    s.setContact(contact);

    Person existing = new Person();
    existing.setKey(UUID.randomUUID());
    existing.setFirstName("First");
    existing.setPosition("foo");
    existing.setEmail("a@a.com");

    StaffDiffFinder staffDiffFinder =
        StaffDiffFinder.builder()
            .entityConverter(ENTITY_CONVERTER)
            .allGrSciCollPersons(Collections.singletonList(existing))
            .build();

    DiffResult.StaffDiffResult<Institution> diffResult =
        staffDiffFinder.syncStaff(
            DEFAULT_INSTITUTION, Collections.singletonList(s), Collections.emptyList());

    assertEquals(1, diffResult.getPersonsToUpdate().size());
    assertEquals(
        s.getPosition(), diffResult.getPersonsToUpdate().get(0).getNewPerson().getPosition());
    assertNotNull(diffResult.getPersonsToUpdate().get(0).getNewPerson().getKey());
  }

  @Test
  public void syncWithGlobalMatchCreateTest() {
    IHStaff s = new IHStaff();
    s.setCode("UARK");
    s.setLastName("Last");
    s.setMiddleName("M.");
    s.setFirstName("First");
    s.setPosition("Collections Manager");

    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setEmail("a@a.com");
    s.setContact(contact);

    StaffDiffFinder staffDiffFinder =
        StaffDiffFinder.builder()
            .entityConverter(ENTITY_CONVERTER)
            .allGrSciCollPersons(Collections.emptyList())
            .build();

    DiffResult.StaffDiffResult<Institution> diffResult =
        staffDiffFinder.syncStaff(
            DEFAULT_INSTITUTION, Collections.singletonList(s), Collections.emptyList());

    assertEquals(1, diffResult.getPersonsToCreate().size());
    assertNull(diffResult.getPersonsToCreate().get(0).getKey());
  }

  @Test
  public void multipleMatchesWithIrnConflictTest() {
    IHStaff s = new IHStaff();
    s.setIrn(IRN_TEST);

    Person p1 = new Person();
    p1.setKey(UUID.randomUUID());
    p1.setEmail("aa@aa.com");
    p1.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    Person p2 = new Person();
    p2.setKey(UUID.randomUUID());
    p2.setEmail("bb@bb.com");
    p2.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    DiffResult.StaffDiffResult<Institution> result =
        STAFF_DIFF_FINDER.syncStaff(
            DEFAULT_INSTITUTION, Collections.singletonList(s), Arrays.asList(p1, p2));

    assertEquals(1, result.getConflicts().size());
  }

  @Test
  public void multipleMatchesWithFieldsConflictTest() {
    IHStaff s = new IHStaff();
    s.setIrn(IRN_TEST);
    s.setFirstName("Name");
    s.setLastName("Last");

    Person p1 = new Person();
    p1.setKey(UUID.randomUUID());
    p1.setFirstName("Name Last");

    Person p2 = new Person();
    p2.setKey(UUID.randomUUID());
    p2.setFirstName("Name");
    p2.setLastName("Last");

    DiffResult.StaffDiffResult<Institution> result =
        STAFF_DIFF_FINDER.syncStaff(
            DEFAULT_INSTITUTION, Collections.singletonList(s), Arrays.asList(p1, p2));

    assertEquals(1, result.getConflicts().size());
  }

  @Test
  public void matchWithFieldsTest() {
    // IH Staff
    IHStaff s = new IHStaff();
    s.setFirstName("First");
    s.setMiddleName("M.");
    s.setLastName("Last");
    s.setPosition("Manager");

    IHStaff.Address ihAddress = new IHStaff.Address();
    ihAddress.setStreet("");
    ihAddress.setCity("Fayetteville");
    ihAddress.setState("AR");
    ihAddress.setCountry("U.S.A.");
    ihAddress.setZipCode("72701");
    s.setAddress(ihAddress);

    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setPhone("[1] 479 575 4372");
    contact.setEmail("a@a.com");
    s.setContact(contact);

    // GrSciColl persons
    Person p1 = new Person();
    p1.setFirstName("First M.");
    p1.setEmail("a@a.com");

    Person p2 = new Person();
    p2.setPosition("Manager");
    Address address = new Address();
    address.setCountry(Country.UNITED_STATES);
    p2.setMailingAddress(address);

    // When
    Set<Person> persons = STAFF_DIFF_FINDER.matchWithFields(s, Arrays.asList(p1, p2), 0);

    // Expect
    assertEquals(1, persons.size());
    assertTrue(persons.contains(p1));

    // GrSciColl persons
    p1 = new Person();
    p1.setFirstName("First");

    p2 = new Person();
    p2.setPosition("Manager");
    address = new Address();
    address.setCountry(Country.UNITED_STATES);
    p2.setMailingAddress(address);

    // When
    persons = STAFF_DIFF_FINDER.matchWithFields(s, Arrays.asList(p1, p2), 0);

    // Expect
    assertEquals(1, persons.size());
    assertTrue(persons.contains(p1));

    // GrSciColl persons
    p1 = new Person();
    p1.setFirstName("Fir");
    p1.setPosition("Manager");

    p2 = new Person();
    p2.setLastName("Last");

    // When
    persons = STAFF_DIFF_FINDER.matchWithFields(s, Arrays.asList(p1, p2), 0);

    // Expect
    assertEquals(1, persons.size());
    assertTrue(persons.contains(p1));
  }

  private TestStaff createTestStaffToUpdate() {
    Person p = new Person();
    p.setKey(UUID.randomUUID());
    p.setFirstName("First M. Last");
    p.setPosition("Director");
    p.setPhone("[1] 479/575-4372");
    p.setEmail("a@uark.edu");
    Address mailingAddress = new Address();
    mailingAddress.setCity("FAYETTEVILLE");
    mailingAddress.setProvince("Arkansas");
    mailingAddress.setCountry(Country.UNITED_STATES);
    p.setMailingAddress(mailingAddress);

    IHStaff s = new IHStaff();
    s.setCode("UARK");
    s.setLastName("Last");
    s.setMiddleName("M.");
    s.setFirstName("First");
    s.setPosition("Professor Emeritus");

    IHStaff.Address address = new IHStaff.Address();
    address.setStreet("");
    address.setCity("Fayetteville");
    address.setState("Arkansas");
    address.setCountry("U.S.A.");
    s.setAddress(address);

    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setEmail("a@uark.edu");
    s.setContact(contact);

    return TestStaff.builder().person(p).ihStaff(s).build();
  }

  private TestStaff createTestStaffNoChange() {
    Person p = new Person();
    p.setKey(UUID.randomUUID());
    p.setEmail("foo@foo.com");
    p.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    IHStaff s = new IHStaff();
    s.setIrn(IRN_TEST);
    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setEmail("foo@foo.com");
    s.setContact(contact);

    return TestStaff.builder().person(p).ihStaff(s).build();
  }

  private TestStaff createTestStaffToDelete() {
    Person p = new Person();
    p.setKey(UUID.randomUUID());
    p.setFirstName("extra person");
    return TestStaff.builder().person(p).build();
  }

  private TestStaff createTestStaffToCreate() {
    IHStaff s = new IHStaff();
    s.setCode("UARK");
    s.setLastName("Last");
    s.setMiddleName("M.");
    s.setFirstName("First");
    s.setPosition("Collections Manager");

    IHStaff.Address address = new IHStaff.Address();
    address.setStreet("");
    address.setCity("Fayetteville");
    address.setState("AR");
    address.setCountry("U.S.A.");
    address.setZipCode("72701");
    s.setAddress(address);

    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setPhone("[1] 479 575 4372");
    contact.setEmail("a@uark.edu");
    s.setContact(contact);

    return TestStaff.builder().ihStaff(s).build();
  }

  @Builder
  @Data
  private static class TestStaff {
    Person person;
    IHStaff ihStaff;
  }
}
