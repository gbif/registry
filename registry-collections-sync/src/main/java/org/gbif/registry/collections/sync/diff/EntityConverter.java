package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
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
import java.util.*;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import static org.gbif.registry.collections.sync.diff.Utils.encodeIRN;

/** Converts IH insitutions to the GrSciColl entities {@link Institution} and {@link Collection}. */
@Slf4j
public class EntityConverter {

  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
  private static final Map<String, Country> COUNTRY_LOOKUP = new HashMap<>();

  private EntityConverter(List<String> countries) {
    matchCountries(countries);

    if (COUNTRY_LOOKUP.size() != countries.size()) {
      log.warn("We couldn't match all the countries to our enum");
    }
  }

  public static EntityConverter from(List<String> countries) {
    return new EntityConverter(countries);
  }

  @VisibleForTesting
  void matchCountries(List<String> countries) {
    Map<String, Country> titleLookup =
        Maps.uniqueIndex(Lists.newArrayList(Country.values()), Country::getTitle);

    countries.forEach(
        c -> {
          Country country = titleLookup.get(c);

          if (c.equalsIgnoreCase("U.K.") || c.equalsIgnoreCase("UK")) {
            country = Country.UNITED_KINGDOM;
          }
          if (country == null) {
            country = Country.fromIsoCode(c);
          }
          if (country == null) {
            country = Country.fromIsoCode(c.replaceAll("\\.", ""));
          }
          if (country == null && c.contains(",")) {
            country = titleLookup.get(c.split(",")[0]);
          }
          if (country == null) {
            country =
                Arrays.stream(Country.values())
                    .filter(v -> c.contains(v.getTitle()))
                    .findFirst()
                    .orElse(null);
          }
          if (country == null) {
            country =
                Arrays.stream(Country.values())
                    .filter(v -> v.getTitle().contains(c))
                    .findFirst()
                    .orElse(null);
          }

          if (country != null) {
            COUNTRY_LOOKUP.put(c, country);
          }
        });
  }

  public Institution convertToInstitution(IHInstitution ihInstitution) {
    return convertToInstitution(ihInstitution, null);
  }

  public Institution convertToInstitution(IHInstitution ihInstitution, Institution existing) {
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

  public Collection convertToCollection(IHInstitution ihInstitution, Collection existing) {
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

  public Person convertToPerson(IHStaff ihStaff) {
    return convertToPerson(ihStaff, null);
  }

  public Person convertToPerson(IHStaff ihStaff, Person existing) {
    Person person = new Person();

    if (existing != null) {
      try {
        BeanUtils.copyProperties(person, existing);
      } catch (IllegalAccessException | InvocationTargetException e) {
        log.warn("Couldn't copy person properties from bean: {}", existing);
      }
    }

    person.setFirstName(buildFirstName(ihStaff));
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
      mailingAddress.setCountry(COUNTRY_LOOKUP.get(ihStaff.getAddress().getCountry()));
      person.setMailingAddress(mailingAddress);
    }

    addIdentifierIfNotExists(person, encodeIRN(ihStaff.getIrn()));

    return person;
  }

  private String buildFirstName(IHStaff ihStaff) {
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

  private static void setAddress(Contactable contactable, IHInstitution ih) {
    Address physicalAddress = null;
    Address mailingAddress = null;
    if (ih.getAddress() != null) {
      physicalAddress = new Address();
      physicalAddress.setAddress(ih.getAddress().getPhysicalStreet());
      physicalAddress.setCity(ih.getAddress().getPhysicalCity());
      physicalAddress.setProvince(ih.getAddress().getPhysicalState());
      physicalAddress.setPostalCode(ih.getAddress().getPhysicalZipCode());
      physicalAddress.setCountry(COUNTRY_LOOKUP.get(ih.getAddress().getPhysicalCountry()));

      mailingAddress = new Address();
      mailingAddress.setAddress(ih.getAddress().getPostalStreet());
      mailingAddress.setCity(ih.getAddress().getPostalCity());
      mailingAddress.setProvince(ih.getAddress().getPostalState());
      mailingAddress.setPostalCode(ih.getAddress().getPostalZipCode());
      mailingAddress.setCountry(COUNTRY_LOOKUP.get(ih.getAddress().getPostalCountry()));
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
      // when there are multiple URLs we try to get the first one
      String webUrl = ih.getContact().getWebUrl();
      if (webUrl.contains(",")) {
        webUrl = ih.getContact().getWebUrl().split(",")[0];
      } else if (webUrl.contains(";")) {
        webUrl = ih.getContact().getWebUrl().split(";")[0];
      }

      // we try to clean the URL...
      webUrl = WHITESPACE.matcher(webUrl).replaceAll(" ");

      try {
        homepage = URI.create(webUrl);
      } catch (Exception ex) {
        log.warn(
            "Couldn't parse the contact webUrl {} for IH institution {}", webUrl, ih.getCode());
      }
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
