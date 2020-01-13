package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;
import org.gbif.registry.collections.sync.ih.IHUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import org.junit.Test;

import static org.gbif.registry.collections.sync.ih.IHUtils.encodeIRN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Tests the {@link IndexHerbariorumDiffFinder}. */
public class IndexHerbariorumDiffFinderTest {

  private static final String IRN_TEST = "1";
  private static final String IRN_TEST_2 = "2";

  private static final Function<String, List<IHStaff>> EMPTY_STAFF = (p) -> Collections.emptyList();

  @Test
  public void syncInstitutionsTest() {
    TestEntity institutionToCreate = createInstitutionToCreate();
    TestEntity institutionNoChange = createInstitutionNoChange();
    TestEntity institutionToUpdate = createInstitutionToUpdate();

    List<TestEntity> allTestEntities =
        ImmutableList.of(institutionToCreate, institutionNoChange, institutionToUpdate);
    List<Institution> grSciCollInstitutions =
        allTestEntities.stream()
            .filter(e -> e.getEntity() != null)
            .map(e -> (Institution) e.getEntity())
            .collect(Collectors.toList());

    List<IHInstitution> ihInstitutions =
        allTestEntities.stream()
            .filter(e -> e.getIhInstitution() != null)
            .map(TestEntity::getIhInstitution)
            .collect(Collectors.toList());

    DiffResult result =
        IndexHerbariorumDiffFinder.syncIH(
            ihInstitutions, EMPTY_STAFF, grSciCollInstitutions, Collections.emptyList());

    assertEquals(1, result.getInstitutionsToCreate().size());
    assertEquals(1, result.getInstitutionsNoChange().size());
    assertEquals(1, result.getInstitutionsToUpdate().size());
    assertTrue(result.getInstitutionConflicts().isEmpty());
    assertTrue(result.getCollectionsToUpdate().isEmpty());
    assertTrue(result.getCollectionsNoChange().isEmpty());
    assertTrue(result.getCollectionConflicts().isEmpty());

    assertFalse(grSciCollInstitutions.contains(result.getInstitutionsToCreate().get(0)));
    assertEquals(institutionNoChange.entity, result.getInstitutionsNoChange().get(0));
    assertNotEquals(
        result.getInstitutionsToUpdate().get(0).getNewInstitution(),
        result.getInstitutionsToUpdate().get(0).getOldInstitution());
    assertTrue(
        result.getInstitutionsToUpdate().get(0).getNewInstitution().isIndexHerbariorumRecord());
  }

  @Test
  public void syncCollectionsTest() {
    TestEntity collectionsNoChange = createCollectionNoChange();
    TestEntity collectionsToUpdate = createCollectionToUpdate();

    List<TestEntity> allTestEntities = ImmutableList.of(collectionsNoChange, collectionsToUpdate);
    List<Collection> grSciCollCollections =
        allTestEntities.stream()
            .filter(e -> e.getEntity() != null)
            .map(e -> (Collection) e.getEntity())
            .collect(Collectors.toList());

    List<IHInstitution> ihInstitutions =
        allTestEntities.stream()
            .filter(e -> e.getIhInstitution() != null)
            .map(TestEntity::getIhInstitution)
            .collect(Collectors.toList());

    DiffResult result =
        IndexHerbariorumDiffFinder.syncIH(
            ihInstitutions,
            (p) -> Collections.emptyList(),
            Collections.emptyList(),
            grSciCollCollections);

    assertEquals(1, result.getCollectionsNoChange().size());
    assertEquals(1, result.getCollectionsToUpdate().size());
    assertTrue(result.getCollectionConflicts().isEmpty());
    assertTrue(result.getInstitutionsToUpdate().isEmpty());
    assertTrue(result.getInstitutionsNoChange().isEmpty());
    assertTrue(result.getInstitutionsToCreate().isEmpty());

    assertEquals(collectionsNoChange.entity, result.getCollectionsNoChange().get(0));
    assertNotEquals(
        result.getCollectionsToUpdate().get(0).getNewCollection(),
        result.getCollectionsToUpdate().get(0).getOldCollection());
    assertTrue(
        result.getCollectionsToUpdate().get(0).getNewCollection().isIndexHerbariorumRecord());
  }

  @Test
  public void outdatedInstitutionConflictTest() {
    IHInstitution outdatedIh = new IHInstitution();
    outdatedIh.setIrn(IRN_TEST);
    outdatedIh.setDateModified("2018-01-01");

    Institution upToDateInstitution = new Institution();
    upToDateInstitution.setModified(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    upToDateInstitution
        .getIdentifiers()
        .add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    DiffResult result =
        IndexHerbariorumDiffFinder.syncIH(
            Collections.singletonList(outdatedIh),
            EMPTY_STAFF,
            Collections.singletonList(upToDateInstitution),
            Collections.emptyList());

    assertEquals(1, result.getInstitutionConflicts().size());
  }

  @Test
  public void outdatedCollectionConflictTest() {
    IHInstitution outdatedIh = new IHInstitution();
    outdatedIh.setIrn(IRN_TEST);
    outdatedIh.setDateModified("2018-01-01");

    Collection upToDateCollection = new Collection();
    upToDateCollection.setModified(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    upToDateCollection
        .getIdentifiers()
        .add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    DiffResult result =
        IndexHerbariorumDiffFinder.syncIH(
            Collections.singletonList(outdatedIh),
            EMPTY_STAFF,
            Collections.emptyList(),
            Collections.singletonList(upToDateCollection));

    assertEquals(1, result.getCollectionConflicts().size());
  }

  @Test
  public void multipleMatchesConflictTest() {
    IHInstitution ih1 = new IHInstitution();
    ih1.setIrn(IRN_TEST);
    ih1.setCode("A");
    IHInstitution ih2 = new IHInstitution();
    ih2.setIrn(IRN_TEST_2);
    ih2.setCode("B");

    Institution i1 = new Institution();
    i1.setCode("i1");
    i1.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    Institution i2 = new Institution();
    i2.setCode("i2");
    i2.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST)));

    Collection c1 = new Collection();
    c1.setCode("c1");
    c1.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST_2)));

    Collection c2 = new Collection();
    c2.setCode("c2");
    c2.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, encodeIRN(IRN_TEST_2)));

    DiffResult result =
        IndexHerbariorumDiffFinder.syncIH(
            Arrays.asList(ih1, ih2), EMPTY_STAFF, Arrays.asList(i1, i2), Arrays.asList(c1, c2));

    assertEquals(2, result.getConflicts().size());
  }

  private TestEntity createInstitutionToCreate() {
    IHInstitution ih = new IHInstitution();
    ih.setCode("foo");
    ih.setOrganization("foo");
    ih.setSpecimenTotal(1000);

    return TestEntity.builder().ihInstitution(ih).build();
  }

  private TestEntity createInstitutionNoChange() {
    Institution i = new Institution();
    i.setCode("bar");
    i.setName("bar");
    i.setIndexHerbariorumRecord(true);
    i.setNumberSpecimens(1000);
    i.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, IHUtils.encodeIRN(IRN_TEST)));

    IHInstitution ih = new IHInstitution();
    ih.setIrn(IRN_TEST);
    ih.setCode("bar");
    ih.setOrganization("bar");
    ih.setSpecimenTotal(1000);

    return TestEntity.builder().entity(i).ihInstitution(ih).build();
  }

  private TestEntity createInstitutionToUpdate() {
    Institution i = new Institution();
    i.setCode("UARK");
    i.setName("University of Arkansas OLD");
    i.setType(InstitutionType.HERBARIUM);
    i.setIndexHerbariorumRecord(false);
    i.setLatitude(new BigDecimal("36.0424"));
    i.setLongitude(new BigDecimal("-94.1624"));
    i.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, IHUtils.encodeIRN(IRN_TEST_2)));

    Address address = new Address();
    address.setCity("FAYETTEVILLE");
    address.setProvince("Arkansas");
    address.setCountry(Country.UNITED_STATES);
    i.setMailingAddress(address);

    IHInstitution ih = new IHInstitution();
    ih.setIrn(IRN_TEST_2);
    ih.setCode("UARK");
    ih.setOrganization("University of Arkansas");
    ih.setSpecimenTotal(1000);

    IHInstitution.Address ihAddress = new IHInstitution.Address();
    ihAddress.setPhysicalCity("FAYETTEVILLE");
    ihAddress.setPhysicalCountry("U.S.A.");
    ihAddress.setPostalCity("FAYETTEVILLE");
    ihAddress.setPostalCountry("U.S.A.");
    ih.setAddress(ihAddress);

    IHInstitution.Location location = new IHInstitution.Location();
    location.setLat(30d);
    location.setLon(-80d);
    ih.setLocation(location);

    IHInstitution.Contact contact = new IHInstitution.Contact();
    contact.setEmail("uark@uark.com");
    ih.setContact(contact);

    return TestEntity.builder().entity(i).ihInstitution(ih).build();
  }

  private TestEntity createCollectionNoChange() {
    Collection c = new Collection();
    c.setCode("A");
    c.setIndexHerbariorumRecord(true);
    c.setEmail(Collections.singletonList("aa@aa.com"));
    c.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, IHUtils.encodeIRN(IRN_TEST)));

    IHInstitution ih = new IHInstitution();
    ih.setIrn(IRN_TEST);
    ih.setCode("A");
    IHInstitution.Contact contact = new IHInstitution.Contact();
    contact.setEmail("aa@aa.com");
    ih.setContact(contact);

    return TestEntity.builder().entity(c).ihInstitution(ih).build();
  }

  private TestEntity createCollectionToUpdate() {
    Collection c = new Collection();
    c.setCode("B");
    c.setEmail(Collections.singletonList("bb@bb.com"));
    c.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, IHUtils.encodeIRN(IRN_TEST_2)));

    IHInstitution ih = new IHInstitution();
    ih.setIrn(IRN_TEST_2);
    ih.setCode("A");

    IHInstitution.Contact contact = new IHInstitution.Contact();
    contact.setEmail("bb@bb.com");
    contact.setPhone("12345");
    ih.setContact(contact);

    return TestEntity.builder().entity(c).ihInstitution(ih).build();
  }

  @Builder
  @Data
  private static class TestEntity {
    CollectionEntity entity;
    IHInstitution ihInstitution;
  }
}
