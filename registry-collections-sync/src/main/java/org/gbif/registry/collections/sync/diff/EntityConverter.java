package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.*;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import static org.gbif.registry.collections.sync.ih.IHUtils.encodeIRN;

/** Converts IH insitutions to the GrSciColl entities {@link Institution} and {@link Collection}. */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityConverter {

  private static final Map<String, Country> TITLE_LOOKUP =
      Maps.uniqueIndex(Lists.newArrayList(Country.values()), Country::getTitle);

  public static InstitutionConverter createInstitution() {
    return new InstitutionConverter();
  }

  public static CollectionConverter createCollection() {
    return new CollectionConverter();
  }

  public static PersonConverter createPerson() {
    return new PersonConverter();
  }

  /** Converts an IH institution to a GrSciColl {@link Institution}. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class InstitutionConverter {

    private IHInstitution ihInstitution;
    private Institution existing;

    public InstitutionConverter fromIHInstitution(IHInstitution ihInstitution) {
      this.ihInstitution = ihInstitution;
      return this;
    }

    public InstitutionConverter withExisting(Institution existing) {
      this.existing = existing;
      return this;
    }

    public Institution convert() {
      Institution institution = new Institution();

      if (existing != null) {
        try {
          BeanUtils.copyProperties(institution, existing);
        } catch (IllegalAccessException | InvocationTargetException e) {
          log.warn("Couldn't copy institution properties from bean: {}", existing);
        }
      }

      institution.setName(ihInstitution.getOrganization());
      institution.setCode(ihInstitution.getCode());
      institution.setIndexHerbariorumRecord(true);
      institution.setNumberSpecimens(Math.toIntExact(ihInstitution.getSpecimenTotal()));
      institution.setLatitude(
          ihInstitution.getLocation() != null
              ? BigDecimal.valueOf(ihInstitution.getLocation().getLat())
              : null);
      institution.setLongitude(
          ihInstitution.getLocation() != null
              ? BigDecimal.valueOf(ihInstitution.getLocation().getLon())
              : null);

      setAddress(institution, ihInstitution);
      institution.setEmail(getIhEmails(ihInstitution));
      institution.setPhone(getIhPhones(ihInstitution));
      institution.setHomepage(getIhHomepage(ihInstitution));

      addIdentifierIfNotExists(institution, encodeIRN(ihInstitution.getIrn()));

      return institution;
    }
  }

  /** Converts an IH institution to a GrSciColl {@link Collection}. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class CollectionConverter {

    private IHInstitution ihInstitution;
    private Collection existing;

    public CollectionConverter fromIHInstitution(IHInstitution ihInstitution) {
      this.ihInstitution = ihInstitution;
      return this;
    }

    public CollectionConverter withExisting(Collection existing) {
      this.existing = existing;
      return this;
    }

    public Collection convert() {
      Collection collection = new Collection();

      if (existing != null) {
        try {
          BeanUtils.copyProperties(collection, existing);
        } catch (IllegalAccessException | InvocationTargetException e) {
          log.warn("Couldn't copy collection properties from bean: {}", existing);
        }
      }

      collection.setName(ihInstitution.getOrganization());
      collection.setCode(ihInstitution.getCode());
      collection.setIndexHerbariorumRecord(true);

      setAddress(collection, ihInstitution);

      collection.setEmail(getIhEmails(ihInstitution));
      collection.setPhone(getIhPhones(ihInstitution));
      collection.setHomepage(getIhHomepage(ihInstitution));

      addIdentifierIfNotExists(collection, encodeIRN(ihInstitution.getIrn()));

      return collection;
    }
  }

  /** Converts an IH staff to a GrSciColl {@link org.gbif.api.model.collections.Person}. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class PersonConverter {

    private IHStaff ihStaff;
    private Person existing;

    public PersonConverter fromIHStaff(IHStaff ihStaff) {
      this.ihStaff = ihStaff;
      return this;
    }

    public PersonConverter withExisting(Person existing) {
      this.existing = existing;
      return this;
    }

    public Person convert() {
      Person person = new Person();

      if (existing != null) {
        try {
          BeanUtils.copyProperties(person, existing);
        } catch (IllegalAccessException | InvocationTargetException e) {
          log.warn("Couldn't copy person properties from bean: {}", existing);
        }
      }

      person.setFirstName(buildFirstName());
      person.setLastName(ihStaff.getLastName());
      person.setPosition(ihStaff.getPosition());

      if (ihStaff.getContact() != null) {
        person.setEmail(ihStaff.getContact().getEmail());
        person.setPhone(ihStaff.getContact().getPhone());
        person.setFax(ihStaff.getContact().getFax());
      }

      if (ihStaff.getAddress() != null) {
        Address mailingAddress = new Address();
        mailingAddress.setAddress(ihStaff.getAddress().getStreet());
        mailingAddress.setCity(ihStaff.getAddress().getCity());
        mailingAddress.setProvince(ihStaff.getAddress().getState());
        mailingAddress.setPostalCode(ihStaff.getAddress().getZipCode());
        mailingAddress.setCountry(TITLE_LOOKUP.get(ihStaff.getAddress().getCountry()));
        person.setMailingAddress(mailingAddress);
      }

      addIdentifierIfNotExists(person, encodeIRN(ihStaff.getIrn()));

      return person;
    }

    private String buildFirstName() {
      StringBuilder firstNameBuilder = new StringBuilder();
      if (!Strings.isNullOrEmpty(ihStaff.getFirstName())) {
        firstNameBuilder.append(ihStaff.getFirstName()).append(" ");
      }
      if (!Strings.isNullOrEmpty(ihStaff.getMiddleName())) {
        firstNameBuilder.append(ihStaff.getMiddleName());
      }

      String firstName = firstNameBuilder.toString();
      if (Strings.isNullOrEmpty(firstName)) {
        return null;
      }

      return firstName.trim();
    }
  }

  private static void setAddress(Contactable contactable, IHInstitution ih) {
    Address physicalAddress = null;
    Address mailingAddress = null;
    if (ih.getAddress() != null) {
      physicalAddress = new Address();
      physicalAddress.setAddress(ih.getAddress().getPhysicalStreet());
      physicalAddress.setCity(ih.getAddress().getPhysicalCity());
      physicalAddress.setProvince(ih.getAddress().getPhysicalState());
      physicalAddress.setPostalCode(ih.getAddress().getPhysicalZipCode());
      physicalAddress.setCountry(TITLE_LOOKUP.get(ih.getAddress().getPhysicalCountry()));

      mailingAddress = new Address();
      mailingAddress.setAddress(ih.getAddress().getPostalStreet());
      mailingAddress.setCity(ih.getAddress().getPostalCity());
      mailingAddress.setProvince(ih.getAddress().getPostalState());
      mailingAddress.setPostalCode(ih.getAddress().getPostalZipCode());
      mailingAddress.setCountry(TITLE_LOOKUP.get(ih.getAddress().getPostalCountry()));
    }
    contactable.setAddress(physicalAddress);
    contactable.setMailingAddress(mailingAddress);
  }

  private static List<String> getIhEmails(IHInstitution ih) {
    List<String> emails = null;
    if (ih.getContact() != null && ih.getContact().getEmail() != null) {
      emails = Arrays.asList(ih.getContact().getEmail().split("\n"));
    }
    return emails;
  }

  private static List<String> getIhPhones(IHInstitution ih) {
    List<String> phones = null;
    if (ih.getContact() != null && ih.getContact().getPhone() != null) {
      phones = Arrays.asList(ih.getContact().getPhone().split("\n"));
    }
    return phones;
  }

  private static URI getIhHomepage(IHInstitution ih) {
    URI homepage = null;
    if (ih.getContact() != null && ih.getContact().getWebUrl() != null) {
      homepage = URI.create(ih.getContact().getWebUrl());
    }
    return homepage;
  }

  private static void addIdentifierIfNotExists(Identifiable entity, String irn) {
    if (!containsIrnAsIdentifier(entity, irn)) {
      // add identifier
      Identifier ihIdentifier = new Identifier(IdentifierType.IH_IRN, irn);
      entity.getIdentifiers().add(ihIdentifier);
    }
  }

  private static boolean containsIrnAsIdentifier(Identifiable entity, String irn) {
    return entity.getIdentifiers().stream().anyMatch(i -> Objects.equals(irn, i.getIdentifier()));
  }
}
