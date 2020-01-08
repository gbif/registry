package org.gbif.registry.collections.sync;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.collections.sync.ih.IhInstitution;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

/** Converts IH insitutions to the GrSciColl entities {@link Institution} and {@link Collection}. */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GrSciCollEntityConverter {

  private static final Map<String, Country> TITLE_LOOKUP =
      Maps.uniqueIndex(Lists.newArrayList(Country.values()), Country::getTitle);

  public static InstitutionConverter createInstitution() {
    return new InstitutionConverter();
  }

  public static CollectionConverter createCollection() {
    return new CollectionConverter();
  }

  /** Converts an IH institution to a GrSciColl {@link Institution}. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class InstitutionConverter {

    private IhInstitution ih;
    private Institution existing;

    public InstitutionConverter fromIHInstitution(IhInstitution ih) {
      this.ih = ih;
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

      institution.setName(ih.getOrganization());
      institution.setCode(ih.getCode());
      institution.setIndexHerbariorumRecord(true);
      institution.setNumberSpecimens(Math.toIntExact(ih.getSpecimenTotal()));
      institution.setLatitude(
          ih.getLocation() != null ? BigDecimal.valueOf(ih.getLocation().getLat()) : null);
      institution.setLongitude(
          ih.getLocation() != null ? BigDecimal.valueOf(ih.getLocation().getLon()) : null);

      setAddress(institution, ih);
      institution.setEmail(getIhEmails(ih));
      institution.setPhone(getIhPhones(ih));
      institution.setHomepage(getIhHomepage(ih));

      return institution;
    }
  }

  /** Converts an IH institution to a GrSciColl {@link Collection}. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class CollectionConverter {

    private IhInstitution ih;
    private Collection existing;

    public CollectionConverter fromIHInstitution(IhInstitution ih) {
      this.ih = ih;
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

      collection.setName(ih.getOrganization());
      collection.setCode(ih.getCode());
      collection.setIndexHerbariorumRecord(true);

      setAddress(collection, ih);

      collection.setEmail(getIhEmails(ih));
      collection.setPhone(getIhPhones(ih));
      collection.setHomepage(getIhHomepage(ih));

      return collection;
    }
  }

  private static void setAddress(Contactable contactable, IhInstitution ih) {
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
      mailingAddress.setAddress(ih.getAddress().getPhysicalStreet());
      mailingAddress.setCity(ih.getAddress().getPostalCity());
      mailingAddress.setProvince(ih.getAddress().getPostalState());
      mailingAddress.setPostalCode(ih.getAddress().getPostalZipCode());
      mailingAddress.setCountry(TITLE_LOOKUP.get(ih.getAddress().getPostalCountry()));
    }
    contactable.setAddress(physicalAddress);
    contactable.setMailingAddress(mailingAddress);
  }

  private static List<String> getIhEmails(IhInstitution ih) {
    List<String> emails = null;
    if (ih.getContact() != null && ih.getContact().getEmail() != null) {
      emails = Arrays.asList(ih.getContact().getEmail().split("\n"));
    }
    return emails;
  }

  private static List<String> getIhPhones(IhInstitution ih) {
    List<String> phones = null;
    if (ih.getContact() != null && ih.getContact().getPhone() != null) {
      phones = Arrays.asList(ih.getContact().getPhone().split("\n"));
    }
    return phones;
  }

  private static URI getIhHomepage(IhInstitution ih) {
    URI homepage = null;
    if (ih.getContact() != null && ih.getContact().getWebUrl() != null) {
      homepage = URI.create(ih.getContact().getWebUrl());
    }
    return homepage;
  }
}
