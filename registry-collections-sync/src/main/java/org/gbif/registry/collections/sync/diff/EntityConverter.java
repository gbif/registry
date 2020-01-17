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
  private static final Map<String, Country> COUNTRY_MANUAL_MAPPINGS = new HashMap<>();
  private final Map<String, Country> countryLookup;

  static {
    COUNTRY_MANUAL_MAPPINGS.put("U.K.", Country.UNITED_KINGDOM);
    COUNTRY_MANUAL_MAPPINGS.put("UK", Country.UNITED_KINGDOM);
    COUNTRY_MANUAL_MAPPINGS.put("Scotland", Country.UNITED_KINGDOM);
    COUNTRY_MANUAL_MAPPINGS.put("Alderney", Country.UNITED_KINGDOM);
    COUNTRY_MANUAL_MAPPINGS.put("Congo Republic (Congo-Brazzaville)", Country.CONGO);
    COUNTRY_MANUAL_MAPPINGS.put("Republic of Congo-Brazzaville", Country.CONGO);
    COUNTRY_MANUAL_MAPPINGS.put("Democratic Republic of the Congo", Country.CONGO_DEMOCRATIC_REPUBLIC);
    COUNTRY_MANUAL_MAPPINGS.put("Czech Republic", Country.CZECH_REPUBLIC);
    COUNTRY_MANUAL_MAPPINGS.put("Italia", Country.ITALY);
    COUNTRY_MANUAL_MAPPINGS.put("Ivory Coast", Country.CÔTE_DIVOIRE);
    COUNTRY_MANUAL_MAPPINGS.put("Laos", Country.LAO);
    COUNTRY_MANUAL_MAPPINGS.put("Republic of Korea", Country.KOREA_SOUTH);
    COUNTRY_MANUAL_MAPPINGS.put("Republic of South Korea", Country.KOREA_SOUTH);
    COUNTRY_MANUAL_MAPPINGS.put("São Tomé e Príncipe", Country.SAO_TOME_PRINCIPE);
    COUNTRY_MANUAL_MAPPINGS.put("Slovak Republic", Country.SLOVAKIA);
    COUNTRY_MANUAL_MAPPINGS.put("Swaziland", Country.SWAZILAND);
    COUNTRY_MANUAL_MAPPINGS.put("Vietnam", Country.VIETNAM);
  }

  private EntityConverter(List<String> countries) {
    countryLookup = mapCountries(countries);

    if (countryLookup.size() != countries.size()) {
      log.warn("We couldn't match all the countries to our enum");
    }
  }

  public static EntityConverter from(List<String> countries) {
    return new EntityConverter(countries);
  }

  @VisibleForTesting
  static Map<String, Country> mapCountries(List<String> countries) {
    // build map with the titles of the Country enum
    Map<String, Country> titleLookup =
        Maps.uniqueIndex(Lists.newArrayList(Country.values()), Country::getTitle);

    Map<String, Country> mappings = new HashMap<>();

    countries.forEach(
        c -> {
          Country country = titleLookup.get(c);

          // we first try manual mappings
          country = COUNTRY_MANUAL_MAPPINGS.get(c);

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
            mappings.put(c, country);
          }
        });

    return mappings;
  }

  public Country matchCountry(String country) {
    return countryLookup.get(country);
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
      person.setEmail(getFirstString(ihStaff.getContact().getEmail()));
      person.setPhone(getFirstString(ihStaff.getContact().getPhone()));
      person.setFax(getFirstString(ihStaff.getContact().getFax()));
    }

    if (ihStaff.getAddress() != null) {
      Address mailingAddress = new Address();
      mailingAddress.setAddress(ihStaff.getAddress().getStreet());
      mailingAddress.setCity(ihStaff.getAddress().getCity());
      mailingAddress.setProvince(ihStaff.getAddress().getState());
      mailingAddress.setPostalCode(ihStaff.getAddress().getZipCode());
      mailingAddress.setCountry(countryLookup.get(ihStaff.getAddress().getCountry()));
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

  private void setAddress(Contactable contactable, IHInstitution ih) {
    Address physicalAddress = null;
    Address mailingAddress = null;
    if (ih.getAddress() != null) {
      physicalAddress = new Address();
      physicalAddress.setAddress(ih.getAddress().getPhysicalStreet());
      physicalAddress.setCity(ih.getAddress().getPhysicalCity());
      physicalAddress.setProvince(ih.getAddress().getPhysicalState());
      physicalAddress.setPostalCode(ih.getAddress().getPhysicalZipCode());
      physicalAddress.setCountry(countryLookup.get(ih.getAddress().getPhysicalCountry()));

      mailingAddress = new Address();
      mailingAddress.setAddress(ih.getAddress().getPostalStreet());
      mailingAddress.setCity(ih.getAddress().getPostalCity());
      mailingAddress.setProvince(ih.getAddress().getPostalState());
      mailingAddress.setPostalCode(ih.getAddress().getPostalZipCode());
      mailingAddress.setCountry(countryLookup.get(ih.getAddress().getPostalCountry()));
    }
    contactable.setAddress(physicalAddress);
    contactable.setMailingAddress(mailingAddress);
  }

  private static List<String> getIhEmails(IHInstitution ih) {
    if (ih.getContact() != null && ih.getContact().getEmail() != null) {
      return parseStringList(ih.getContact().getEmail());
    }
    return Collections.emptyList();
  }

  private static List<String> getIhPhones(IHInstitution ih) {
    if (ih.getContact() != null && ih.getContact().getPhone() != null) {
      return parseStringList(ih.getContact().getPhone());
    }
    return Collections.emptyList();
  }

  private static List<String> parseStringList(String stringList) {
    String listNormalized = stringList.replaceAll("\n", ",");
    return Arrays.asList(listNormalized.split(","));
  }

  private static URI getIhHomepage(IHInstitution ih) {
    URI homepage = null;
    if (ih.getContact() != null && ih.getContact().getWebUrl() != null) {
      // when there are multiple URLs we try to get the first one
      String webUrl = getFirstString(ih.getContact().getWebUrl());

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

  private static String getFirstString(String stringList) {
    if (stringList.contains(",")) {
      return stringList.split(",")[0];
    } else if (stringList.contains(";")) {
      return stringList.split(";")[0];
    } else if (stringList.contains("\n")) {
      return stringList.split("\n")[0];
    }
    return stringList;
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
