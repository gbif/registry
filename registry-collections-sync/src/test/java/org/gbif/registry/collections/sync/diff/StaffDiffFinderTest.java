package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import org.junit.Test;

import static org.gbif.registry.collections.sync.ih.IHUtils.encodeIRN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/** Tests the {@link] StaffDiffFinder}. */
public class StaffDiffFinderTest {

  private static final String IRN_TEST = "1";

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

    DiffResult.StaffDiffResult result = StaffDiffFinder.syncStaff(ihStaff, grSciCollPersons);
    assertEquals(1, result.getPersonsToCreate().size());
    assertEquals(1, result.getPersonsToUpdate().size());
    assertEquals(1, result.getPersonsNoChange().size());
    assertEquals(1, result.getPersonsToDelete().size());

    assertEquals(staffNoChange.person, result.getPersonsNoChange().get(0));
    assertNotEquals(
        result.getPersonsToUpdate().get(0).getNewPerson(),
        result.getPersonsToUpdate().get(0).getOldPerson());
    assertEquals(staffToDelete.person, result.getPersonsToDelete().get(0));
    assertFalse(grSciCollPersons.contains(result.getPersonsToCreate().get(0)));
  }

  @Test
  public void outdatedIHConflictTest() {
    IHStaff outdatedStaff =
        IHStaff.builder()
            .irn(IRN_TEST)
            .dateModified("2018-01-01")
            .build();

    Person upToDatePerson = new Person();
    upToDatePerson.setModified(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    upToDatePerson.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    DiffResult.StaffDiffResult result =
        StaffDiffFinder.syncStaff(
            Collections.singletonList(outdatedStaff), Collections.singletonList(upToDatePerson));

    assertEquals(1, result.getConflicts().size());
  }

  @Test
  public void multipleMatchesWithIrnConflictTest() {
    IHStaff s = IHStaff.builder().irn(IRN_TEST).build();

    Person p1 = new Person();
    p1.setEmail("aa@aa.com");
    p1.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    Person p2 = new Person();
    p2.setEmail("bb@bb.com");
    p2.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    DiffResult.StaffDiffResult result =
        StaffDiffFinder.syncStaff(Collections.singletonList(s), Arrays.asList(p1, p2));

    assertEquals(1, result.getConflicts().size());
  }

  @Test
  public void multipleMatchesWithFieldsConflictTest() {
    IHStaff s = IHStaff.builder().irn(IRN_TEST).firstName("Name").lastName("Last").build();

    Person p1 = new Person();
    p1.setFirstName("Name Last");

    Person p2 = new Person();
    p2.setFirstName("Name");
    p2.setLastName("Last");

    DiffResult.StaffDiffResult result =
      StaffDiffFinder.syncStaff(Collections.singletonList(s), Arrays.asList(p1, p2));

    assertEquals(1, result.getConflicts().size());
  }

  private TestStaff createTestStaffToUpdate() {
    Person p = new Person();
    p.setFirstName("Johnnie L. Gentry");
    p.setPosition("Director");
    p.setPhone("[1] 479/575-4372");
    p.setEmail("gentry@uark.edu");
    Address mailingAddress = new Address();
    mailingAddress.setCity("FAYETTEVILLE");
    mailingAddress.setProvince("Arkansas");
    mailingAddress.setCountry(Country.UNITED_STATES);
    p.setMailingAddress(mailingAddress);

    IHStaff s =
        IHStaff.builder()
            .code("UARK")
            .lastName("Gentry")
            .middleName("L.")
            .firstName("Johnnie")
            .position("Professor Emeritus")
            .address(
                IHStaff.Address.builder()
                    .street("")
                    .city("Fayetteville")
                    .state("Arkansas")
                    .country("U.S.A.")
                    .build())
            .contact(IHStaff.Contact.builder().email("gentry@uark.edu").build())
            .build();

    return TestStaff.builder().person(p).ihStaff(s).build();
  }

  private TestStaff createTestStaffNoChange() {
    Person p = new Person();
    p.setEmail("foo@foo.com");
    p.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    IHStaff s =
        IHStaff.builder()
            .irn(IRN_TEST)
            .contact(IHStaff.Contact.builder().email("foo@foo.com").build())
            .build();

    return TestStaff.builder().person(p).ihStaff(s).build();
  }

  private TestStaff createTestStaffToDelete() {
    Person p = new Person();
    p.setFirstName("extra person");
    return TestStaff.builder().person(p).build();
  }

  private TestStaff createTestStaffToCreate() {
    IHStaff s =
        IHStaff.builder()
            .code("UARK")
            .lastName("Ogle")
            .middleName("D.")
            .firstName("Jennifer")
            .position("Collections Manager")
            .address(
                IHStaff.Address.builder()
                    .street("")
                    .city("Fayetteville")
                    .state("AR")
                    .country("U.S.A.")
                    .zipCode("72701")
                    .build())
            .contact(
                IHStaff.Contact.builder().phone("[1] 479 575 4372").email("jogle@uark.edu").build())
            .build();

    return TestStaff.builder().ihStaff(s).build();
  }

  @Builder
  @Data
  private static class TestStaff {
    Person person;
    IHStaff ihStaff;
  }
}
