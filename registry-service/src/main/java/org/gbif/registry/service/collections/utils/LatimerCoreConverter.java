package org.gbif.registry.service.collections.utils;

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.collections.latimercore.Address;
import org.gbif.api.model.collections.latimercore.CollectionStatusHistory;
import org.gbif.api.model.collections.latimercore.ContactDetail;
import org.gbif.api.model.collections.latimercore.GeographicContext;
import org.gbif.api.model.collections.latimercore.Identifier;
import org.gbif.api.model.collections.latimercore.MeasurementOrFact;
import org.gbif.api.model.collections.latimercore.ObjectClassification;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.latimercore.OrganisationalUnit;
import org.gbif.api.model.collections.latimercore.Person;
import org.gbif.api.model.collections.latimercore.PersonRole;
import org.gbif.api.model.collections.latimercore.Reference;
import org.gbif.api.model.collections.latimercore.ResourceRelationship;
import org.gbif.api.model.collections.latimercore.Role;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.elasticsearch.common.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LatimerCoreConverter {

  public static final String DELIMITER = "\\|";
  public static final String PHONE = "Phone";
  public static final String EMAIL = "Email";
  public static final String PHYSICAL = "Physical";
  public static final String MAILING = "Mailing";
  public static final String ACTIVE = "Active";
  public static final String NOT_ACTIVE = "Not active";
  public static final String PRIMARY = "Primary";
  public static final String PART_OF = "part of";
  public static final String TAXONOMIC_SCOPE = "Taxonomic scope";

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class IdentifierTypes {
    public static final String INSTITUTION_CODE = "Institution code";
    public static final String INSTITUTION_GRSCICOLL_KEY = "Institution GRSciColl key";
    public static final String ALTERNATIVE_INSTITUTION_CODE = "Alternative institution code";
    public static final String COLLECTION_CODE = "Collection code";
    public static final String COLLECTION_GRSCICOLL_KEY = "Collection GRSciColl key";
    public static final String ALTERNATIVE_COLLECTION_CODE = "Alternative collection code";
    public static final String CONTACT_GRSCICOLL_KEY = "Contact GRSciColl key";
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class MeasurementOrFactTypes {
    public static final String INSTITUTION_DESCRIPTION = "Institution description";
    public static final String INSTITUTION_TYPE = "Institution type";
    public static final String INSTITUTION_STATUS = "Institution status";
    public static final String INSTITUTION_GOVERNANCE_TYPE = "Institution governance type";
    public static final String RESEARCH_DISCIPLINE = "Research discipline";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String ADDITIONAL_NAME = "Additional name";
    public static final String YEAR_FOUNDED = "Year founded";
    public static final String DISPLAY_ON_NHC_PORTAL = "Display on NHC portal";
    public static final String COLLECTION_NOTE = "Collection note";
    public static final String NUMBER_SPECIMENS = "Number of specimens";
    public static final String TAXONOMIC_EXPERTISE = "Taxonomic expertise";
    public static final String CONTACT_NOTE = "Contact note";
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class References {
    public static final String WEBSITE = "Website";
    public static final String API = "API";
    public static final String IMAGE = "Image";
    public static final String INSTITUTION_HOMEPAGE = "Institution homepage";
    public static final String INSTITUTION_COLLECTION_CATALOGUE =
        "Institution collection catalogue";
    public static final String INSTITUTION_COLLECTION_API = "Institution collection API";
    public static final String INSTITUTION_LOGO_URL = "Institution logo URL";
    public static final String COLLECTION_HOMEPAGE = "Collection homepage";
    public static final String COLLECTION_CATALOGUE = "Collection catalogue";
    public static final String COLLECTION_API = "Collection API";
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class CollectionStatuses {
    public static final String ACCESSION_STATUS = "Accession status";
    public static final String OWNERSHIP = "Ownership";
    public static final String PERSONAL_COLLECTION = "Personal collection";
  }

  public static OrganisationalUnit toOrganisationalUnit(Institution institution) {
    OrganisationalUnit organisationalUnit = new OrganisationalUnit();
    organisationalUnit.setOrganisationalUnitName(institution.getName());
    organisationalUnit.setOrganisationalUnitType("Institution");

    organisationalUnit
        .getIdentifier()
        .add(
            createIdentifier(
                institution.getKey().toString(), IdentifierTypes.INSTITUTION_GRSCICOLL_KEY));

    if (institution.getAddress() != null) {
      organisationalUnit.getAddress().add(toLatimerCoreAddress(institution.getAddress(), PHYSICAL));
    }
    if (institution.getMailingAddress() != null) {
      organisationalUnit
          .getAddress()
          .add(toLatimerCoreAddress(institution.getMailingAddress(), MAILING));
    }

    if (institution.getPhone() != null && !institution.getPhone().isEmpty()) {
      institution
          .getPhone()
          .forEach(p -> organisationalUnit.getContactDetail().add(createContactDetail(p, PHONE)));
    }

    if (institution.getEmail() != null && !institution.getEmail().isEmpty()) {
      institution
          .getEmail()
          .forEach(e -> organisationalUnit.getContactDetail().add(createContactDetail(e, EMAIL)));
    }

    organisationalUnit
        .getIdentifier()
        .add(createIdentifier(institution.getCode(), IdentifierTypes.INSTITUTION_CODE));

    if (institution.getAlternativeCodes() != null && !institution.getAlternativeCodes().isEmpty()) {
      institution
          .getAlternativeCodes()
          .forEach(
              ac ->
                  organisationalUnit
                      .getIdentifier()
                      .add(
                          createIdentifier(
                              ac.getCode(), IdentifierTypes.ALTERNATIVE_INSTITUTION_CODE)));
    }

    addMeasurementOrFactText(
        institution.getDescription(),
        MeasurementOrFactTypes.INSTITUTION_DESCRIPTION,
        organisationalUnit.getMeasurementOrFact());
    institution
        .getTypes()
        .forEach(
            t ->
                addMeasurementOrFactText(
                    t,
                    MeasurementOrFactTypes.INSTITUTION_TYPE,
                    organisationalUnit.getMeasurementOrFact()));
    addMeasurementOrFactText(
        institution.isActive() ? ACTIVE : NOT_ACTIVE,
        MeasurementOrFactTypes.INSTITUTION_STATUS,
        organisationalUnit.getMeasurementOrFact());
    institution
        .getInstitutionalGovernances()
        .forEach(
            ig ->
                addMeasurementOrFactText(
                    ig,
                    MeasurementOrFactTypes.INSTITUTION_GOVERNANCE_TYPE,
                    organisationalUnit.getMeasurementOrFact()));
    institution
        .getDisciplines()
        .forEach(
            d ->
                addMeasurementOrFactText(
                    d,
                    MeasurementOrFactTypes.RESEARCH_DISCIPLINE,
                    organisationalUnit.getMeasurementOrFact()));
    addMeasurementOrFactValue(
        institution.getLatitude(),
        MeasurementOrFactTypes.LATITUDE,
        organisationalUnit.getMeasurementOrFact());
    addMeasurementOrFactValue(
        institution.getLongitude(),
        MeasurementOrFactTypes.LONGITUDE,
        organisationalUnit.getMeasurementOrFact());
    institution
        .getAdditionalNames()
        .forEach(
            n ->
                addMeasurementOrFactText(
                    n,
                    MeasurementOrFactTypes.ADDITIONAL_NAME,
                    organisationalUnit.getMeasurementOrFact()));
    addMeasurementOrFactValue(
        institution.getFoundingDate(),
        MeasurementOrFactTypes.YEAR_FOUNDED,
        organisationalUnit.getMeasurementOrFact());
    addMeasurementOrFactText(
        institution.getDisplayOnNHCPortal().toString(),
        MeasurementOrFactTypes.DISPLAY_ON_NHC_PORTAL,
        organisationalUnit.getMeasurementOrFact());
    addReference(
        institution.getHomepage(),
        References.INSTITUTION_HOMEPAGE,
        References.WEBSITE,
        organisationalUnit.getReference());
    institution
        .getCatalogUrls()
        .forEach(
            c ->
                addReference(
                    c,
                    References.INSTITUTION_COLLECTION_CATALOGUE,
                    References.WEBSITE,
                    organisationalUnit.getReference()));
    institution
        .getApiUrls()
        .forEach(
            a ->
                addReference(
                    a,
                    References.INSTITUTION_COLLECTION_API,
                    References.API,
                    organisationalUnit.getReference()));
    addReference(
        institution.getLogoUrl(),
        References.INSTITUTION_LOGO_URL,
        References.IMAGE,
        organisationalUnit.getReference());

    return organisationalUnit;
  }

  public static Institution fromOrganisationalUnit(OrganisationalUnit organisationalUnit) {
    Institution institution = new Institution();

    getInstitutionKey(organisationalUnit).ifPresent(institution::setKey);

    institution.setName(organisationalUnit.getOrganisationalUnitName());

    organisationalUnit
        .getAddress()
        .forEach(
            a -> {
              if (PHYSICAL.equals(a.getAddressType())) {
                institution.setAddress(toCollectionsAddress(a));
              } else if (MAILING.equals(a.getAddressType())) {
                institution.setMailingAddress(toCollectionsAddress(a));
              }
            });

    convertContactDetails(organisationalUnit.getContactDetail(), institution);

    organisationalUnit
        .getIdentifier()
        .forEach(
            i -> {
              if (IdentifierTypes.INSTITUTION_CODE.equals(i.getIdentifierType())) {
                institution.setCode(i.getIdentifierValue());
              } else if (IdentifierTypes.ALTERNATIVE_INSTITUTION_CODE.equals(
                  i.getIdentifierType())) {
                institution
                    .getAlternativeCodes()
                    .add(new AlternativeCode(i.getIdentifierValue(), null));
              }
            });

    organisationalUnit
        .getMeasurementOrFact()
        .forEach(
            m -> {
              if (MeasurementOrFactTypes.INSTITUTION_DESCRIPTION.equals(m.getMeasurementType())) {
                institution.setDescription(m.getMeasurementFactText());
              } else if (MeasurementOrFactTypes.INSTITUTION_TYPE.equals(m.getMeasurementType())) {
                institution.getTypes().add(m.getMeasurementFactText());
              } else if (MeasurementOrFactTypes.INSTITUTION_STATUS.equals(m.getMeasurementType())) {
                institution.setActive(ACTIVE.equals(m.getMeasurementFactText()));
              } else if (MeasurementOrFactTypes.INSTITUTION_GOVERNANCE_TYPE.equals(
                  m.getMeasurementType())) {
                institution.getInstitutionalGovernances().add(m.getMeasurementFactText());
              } else if (MeasurementOrFactTypes.RESEARCH_DISCIPLINE.equals(
                  m.getMeasurementType())) {
                institution.getDisciplines().add(m.getMeasurementFactText());
              } else if (MeasurementOrFactTypes.LATITUDE.equals(m.getMeasurementType())) {
                institution.setLatitude(new BigDecimal(m.getMeasurementValue()));
              } else if (MeasurementOrFactTypes.LONGITUDE.equals(m.getMeasurementType())) {
                institution.setLongitude(new BigDecimal(m.getMeasurementValue()));
              } else if (MeasurementOrFactTypes.ADDITIONAL_NAME.equals(m.getMeasurementType())) {
                institution.getAdditionalNames().add(m.getMeasurementFactText());
              } else if (MeasurementOrFactTypes.YEAR_FOUNDED.equals(m.getMeasurementType())) {
                institution.setFoundingDate(Integer.parseInt(m.getMeasurementValue()));
              } else if (MeasurementOrFactTypes.DISPLAY_ON_NHC_PORTAL.equals(
                  m.getMeasurementType())) {
                institution.setDisplayOnNHCPortal(Boolean.valueOf(m.getMeasurementFactText()));
              }
            });

    organisationalUnit
        .getReference()
        .forEach(
            r -> {
              if (References.INSTITUTION_HOMEPAGE.equals(r.getReferenceName())) {
                institution.setHomepage(r.getResourceIRI());
              } else if (References.INSTITUTION_COLLECTION_CATALOGUE.equals(r.getReferenceName())) {
                institution.getCatalogUrls().add(r.getResourceIRI());
              } else if (References.INSTITUTION_COLLECTION_API.equals(r.getReferenceName())) {
                institution.getApiUrls().add(r.getResourceIRI());
              } else if (References.INSTITUTION_LOGO_URL.equals(r.getReferenceName())) {
                institution.setLogoUrl(r.getResourceIRI());
              }
            });

    return institution;
  }

  private static void convertContactDetails(
      List<ContactDetail> contactDetails, CollectionEntity entity) {
    contactDetails.forEach(
        c -> {
          if (PHONE.equals(c.getContactDetailCategory())) {
            entity.getPhone().add(c.getContactDetailValue());
          } else if (EMAIL.equals(c.getContactDetailCategory())) {
            entity.getEmail().add(c.getContactDetailValue());
          }
        });
  }

  public static ObjectGroup toObjectGroup(
      CollectionView collectionView, ConceptClient conceptClient) {
    ObjectGroup objectGroup = new ObjectGroup();
    Collection collection = collectionView.getCollection();
    objectGroup.setCollectionName(collection.getName());
    objectGroup.setDescription(collection.getDescription());

    objectGroup
        .getIdentifier()
        .add(
            createIdentifier(
                collection.getKey().toString(), IdentifierTypes.COLLECTION_GRSCICOLL_KEY));

    collection
        .getContentTypes()
        .forEach(
            ct -> {
              ConceptView conceptView =
                  conceptClient.getFromLatestRelease(
                      Vocabularies.COLLECTION_CONTENT_TYPE, ct, false, false);
              if (conceptView != null
                  && conceptView.getConcept().getTags().stream()
                      .anyMatch(t -> t.getName().equals("ltc:discipline"))) {
                objectGroup.getDiscipline().add(ct);
              } else {
                objectGroup.getTypeOfObjectGroup().add(ct);
              }
            });

    OrganisationalUnit organisationalUnit = new OrganisationalUnit();
    organisationalUnit.setOrganisationalUnitName(collectionView.getInstitutionName());
    organisationalUnit.setOrganisationalUnitType("Institution");
    organisationalUnit
        .getIdentifier()
        .add(
            createIdentifier(
                collectionView.getInstitutionCode(), IdentifierTypes.INSTITUTION_CODE));
    objectGroup.getHasOrganisationalUnit().add(organisationalUnit);

    if (collection.getInstitutionKey() != null) {
      organisationalUnit
          .getIdentifier()
          .add(
              createIdentifier(
                  collection.getInstitutionKey().toString(),
                  IdentifierTypes.INSTITUTION_GRSCICOLL_KEY));
    }

    objectGroup.setIsCurrentCollection(collection.isActive());
    collection.getPreservationTypes().forEach(v -> objectGroup.getPreservationMethod().add(v));

    if (collection.getAddress() != null) {
      objectGroup.getAddress().add(toLatimerCoreAddress(collection.getAddress(), PHYSICAL));
    }
    if (collection.getMailingAddress() != null) {
      objectGroup.getAddress().add(toLatimerCoreAddress(collection.getMailingAddress(), MAILING));
    }

    if (collection.getAccessionStatus() != null) {
      addCollectionStatusHistory(
          collection.getAccessionStatus(), CollectionStatuses.ACCESSION_STATUS, objectGroup);
    }
    if (Boolean.TRUE.equals(collection.isPersonalCollection())) {
      addCollectionStatusHistory(
          CollectionStatuses.PERSONAL_COLLECTION, CollectionStatuses.OWNERSHIP, objectGroup);
    }

    if (collection.getPhone() != null && !collection.getPhone().isEmpty()) {
      collection
          .getPhone()
          .forEach(p -> objectGroup.getContactDetail().add(createContactDetail(p, PHONE)));
    }

    if (collection.getEmail() != null && !collection.getEmail().isEmpty()) {
      collection
          .getEmail()
          .forEach(e -> objectGroup.getContactDetail().add(createContactDetail(e, EMAIL)));
    }

    if (!Strings.isNullOrEmpty(collection.getGeographicCoverage())) {
      GeographicContext geographicContext = new GeographicContext();
      objectGroup.getGeographicContext().add(geographicContext);
      addMeasurementOrFactText(
          collection.getGeographicCoverage(),
          "Geographic coverage",
          geographicContext.getHasMeasurementOrFact());
    }

    objectGroup
        .getIdentifier()
        .add(createIdentifier(collection.getCode(), IdentifierTypes.COLLECTION_CODE));
    collection
        .getAlternativeCodes()
        .forEach(
            ac ->
                objectGroup
                    .getIdentifier()
                    .add(
                        createIdentifier(
                            collection.getCode(), IdentifierTypes.ALTERNATIVE_COLLECTION_CODE)));

    addMeasurementOrFactText(
        collection.getNotes(),
        MeasurementOrFactTypes.COLLECTION_NOTE,
        objectGroup.getMeasurementOrFact());
    addMeasurementOrFactText(
        collection.getDisplayOnNHCPortal().toString(),
        MeasurementOrFactTypes.DISPLAY_ON_NHC_PORTAL,
        objectGroup.getMeasurementOrFact());

    addMeasurementOrFactValue(
        collection.getNumberSpecimens(),
        MeasurementOrFactTypes.NUMBER_SPECIMENS,
        objectGroup.getMeasurementOrFact());

    addReference(
        collection.getHomepage(),
        References.COLLECTION_HOMEPAGE,
        References.WEBSITE,
        objectGroup.getReference());
    collection
        .getCatalogUrls()
        .forEach(
            c ->
                addReference(
                    c,
                    References.COLLECTION_CATALOGUE,
                    References.WEBSITE,
                    objectGroup.getReference()));
    collection
        .getApiUrls()
        .forEach(
            u ->
                addReference(
                    u, References.COLLECTION_API, References.API, objectGroup.getReference()));

    collection
        .getIncorporatedCollections()
        .forEach(
            c -> {
              ResourceRelationship resourceRelationship = new ResourceRelationship();
              resourceRelationship.setRelatedResourceName(c);
              resourceRelationship.setRelationshipOfResource(PART_OF);
              objectGroup.getResourceRelationship().add(resourceRelationship);
            });

    if (!Strings.isNullOrEmpty(collection.getTaxonomicCoverage())) {
      ObjectClassification objectClassification = new ObjectClassification();
      objectClassification.setObjectClassificationName(collection.getTaxonomicCoverage());
      objectClassification.setObjectClassificationLevel(TAXONOMIC_SCOPE);
      objectGroup.getObjectClassification().add(objectClassification);
    }

    collection
        .getContactPersons()
        .forEach(
            cp -> {
              PersonRole personRole = new PersonRole();
              objectGroup.getPersonRole().add(personRole);

              Person person = new Person();
              personRole.getPerson().add(person);
              person.setGivenName(cp.getFirstName());
              person.setFamilyName(cp.getLastName());

              Address address = new Address();
              address.setStreetAddress(String.join(DELIMITER, cp.getAddress()));
              address.setAddressLocality(cp.getCity());
              address.setAddressRegion(cp.getProvince());
              address.setPostalCode(cp.getPostalCode());
              address.setAddressCountry(cp.getCountry());
              person.getAddress().add(address);

              person
                  .getContactDetail()
                  .add(createContactDetail(String.join(DELIMITER, cp.getEmail()), EMAIL));
              person
                  .getContactDetail()
                  .add(createContactDetail(String.join(DELIMITER, cp.getPhone()), PHONE));

              if (cp.getKey() != null) {
                person
                    .getIdentifier()
                    .add(
                        createIdentifier(
                            cp.getKey().toString(), IdentifierTypes.CONTACT_GRSCICOLL_KEY));
              }

              cp.getUserIds()
                  .forEach(
                      u ->
                          person
                              .getIdentifier()
                              .add(createIdentifier(u.getId(), u.getType().name())));

              addMeasurementOrFactText(
                  String.join(DELIMITER, cp.getTaxonomicExpertise()),
                  MeasurementOrFactTypes.TAXONOMIC_EXPERTISE,
                  person.getMeasurementOrFact());

              personRole.getRole().add(new Role(String.join(DELIMITER, cp.getPosition())));
              if (cp.isPrimary()) {
                personRole.getRole().add(new Role(PRIMARY));
              }

              addMeasurementOrFactText(
                  cp.getNotes(),
                  MeasurementOrFactTypes.CONTACT_NOTE,
                  personRole.getMeasurementOrFact());
            });

    return objectGroup;
  }

  public static Collection fromObjectGroup(ObjectGroup objectGroup) {
    Collection collection = new Collection();
    collection.setName(objectGroup.getCollectionName());
    collection.setDescription(objectGroup.getDescription());

    getCollectionKey(objectGroup).ifPresent(collection::setKey);

    objectGroup.getDiscipline().forEach(d -> collection.getContentTypes().add(d));
    objectGroup.getTypeOfObjectGroup().forEach(d -> collection.getContentTypes().add(d));

    if (!objectGroup.getHasOrganisationalUnit().isEmpty()) {
      objectGroup
          .getHasOrganisationalUnit()
          .get(0)
          .getIdentifier()
          .forEach(
              i -> {
                if (IdentifierTypes.INSTITUTION_GRSCICOLL_KEY.equals(i.getIdentifierType())) {
                  collection.setInstitutionKey(UUID.fromString(i.getIdentifierValue()));
                }
              });
    }

    collection.setActive(
        Boolean.TRUE.equals(objectGroup.getIsCurrentCollection()) ? Boolean.TRUE : Boolean.FALSE);
    objectGroup.getPreservationMethod().forEach(p -> collection.getPreservationTypes().add(p));

    objectGroup
        .getAddress()
        .forEach(
            a -> {
              if (PHYSICAL.equals(a.getAddressType())) {
                collection.setAddress(toCollectionsAddress(a));
              } else if (MAILING.equals(a.getAddressType())) {
                collection.setMailingAddress(toCollectionsAddress(a));
              }
            });

    objectGroup
        .getCollectionStatusHistory()
        .forEach(
            s -> {
              if (CollectionStatuses.ACCESSION_STATUS.equals(s.getStatusType())) {
                collection.setAccessionStatus(s.getStatus());
              } else if (CollectionStatuses.OWNERSHIP.equals(s.getStatusType())) {
                collection.setPersonalCollection(true);
              }
            });

    convertContactDetails(objectGroup.getContactDetail(), collection);

    if (!objectGroup.getGeographicContext().isEmpty()
        && !objectGroup.getGeographicContext().get(0).getHasMeasurementOrFact().isEmpty()) {
      collection.setGeographicCoverage(
          objectGroup
              .getGeographicContext()
              .get(0)
              .getHasMeasurementOrFact()
              .get(0)
              .getMeasurementFactText());
    }

    objectGroup
        .getIdentifier()
        .forEach(
            i -> {
              if (IdentifierTypes.COLLECTION_CODE.equals(i.getIdentifierType())) {
                collection.setCode(i.getIdentifierValue());
              } else if (IdentifierTypes.ALTERNATIVE_COLLECTION_CODE.equals(
                  i.getIdentifierType())) {
                collection
                    .getAlternativeCodes()
                    .add(new AlternativeCode(i.getIdentifierValue(), null));
              }
            });

    objectGroup
        .getMeasurementOrFact()
        .forEach(
            m -> {
              if (MeasurementOrFactTypes.COLLECTION_NOTE.equals(m.getMeasurementType())) {
                collection.setNotes(m.getMeasurementFactText());
              } else if (MeasurementOrFactTypes.DISPLAY_ON_NHC_PORTAL.equals(
                  m.getMeasurementType())) {
                collection.setDisplayOnNHCPortal(Boolean.valueOf(m.getMeasurementFactText()));
              } else if (MeasurementOrFactTypes.NUMBER_SPECIMENS.equals(m.getMeasurementType())) {
                collection.setNumberSpecimens(Integer.parseInt(m.getMeasurementValue()));
              }
            });

    objectGroup
        .getPersonRole()
        .forEach(
            pr -> {
              Contact contact = new Contact();
              collection.getContactPersons().add(contact);
              pr.getPerson()
                  .forEach(
                      p -> {
                        contact.setFirstName(p.getGivenName());
                        contact.setLastName(p.getFamilyName());

                        if (!p.getAddress().isEmpty()) {
                          Address address = p.getAddress().get(0);
                          contact.setAddress(
                              Arrays.asList(address.getStreetAddress().split(DELIMITER)));
                          contact.setCity(address.getAddressLocality());
                          contact.setProvince(address.getAddressRegion());
                          contact.setPostalCode(address.getPostalCode());
                          contact.setCountry(address.getAddressCountry());
                        }

                        p.getContactDetail()
                            .forEach(
                                c -> {
                                  if (PHONE.equals(c.getContactDetailCategory())) {
                                    contact.setPhone(
                                        Arrays.asList(c.getContactDetailValue().split(DELIMITER)));
                                  } else if (EMAIL.equals(c.getContactDetailCategory())) {
                                    contact.setEmail(
                                        Arrays.asList(c.getContactDetailValue().split(DELIMITER)));
                                  }
                                });

                        p.getIdentifier()
                            .forEach(
                                i -> {
                                  if (IdentifierTypes.CONTACT_GRSCICOLL_KEY.equals(
                                      i.getIdentifierType())) {
                                    contact.setKey(Integer.parseInt(i.getIdentifierValue()));
                                  } else {
                                    UserId userId = new UserId();
                                    userId.setType(IdType.valueOf(i.getIdentifierType()));
                                    userId.setId(i.getIdentifierValue());
                                    contact.getUserIds().add(userId);
                                  }
                                });

                        p.getMeasurementOrFact().stream()
                            .filter(
                                m ->
                                    MeasurementOrFactTypes.TAXONOMIC_EXPERTISE.equals(
                                        m.getMeasurementType()))
                            .findFirst()
                            .ifPresent(
                                v ->
                                    contact.setTaxonomicExpertise(
                                        Arrays.asList(
                                            v.getMeasurementFactText().split(DELIMITER))));
                      });

              pr.getRole()
                  .forEach(
                      r -> {
                        if (PRIMARY.equals(r.getRoleName())) {
                          contact.setPrimary(true);
                        } else {
                          contact.setPosition(Arrays.asList(r.getRoleName().split(DELIMITER)));
                        }
                      });

              pr.getMeasurementOrFact().stream()
                  .filter(m -> MeasurementOrFactTypes.CONTACT_NOTE.equals(m.getMeasurementType()))
                  .findFirst()
                  .ifPresent(v -> contact.setNotes(v.getMeasurementFactText()));
            });

    objectGroup
        .getReference()
        .forEach(
            r -> {
              if (References.COLLECTION_HOMEPAGE.equals(r.getReferenceName())) {
                collection.setHomepage(r.getResourceIRI());
              } else if (References.COLLECTION_CATALOGUE.equals(r.getReferenceName())) {
                collection.getCatalogUrls().add(r.getResourceIRI());
              } else if (References.COLLECTION_API.equals(r.getReferenceName())) {
                collection.getApiUrls().add(r.getResourceIRI());
              }
            });

    collection.setIncorporatedCollections(
        objectGroup.getResourceRelationship().stream()
            .filter(r -> PART_OF.equals(r.getRelationshipOfResource()))
            .map(ResourceRelationship::getRelatedResourceName)
            .collect(Collectors.toList()));

    objectGroup.getObjectClassification().stream()
        .filter(o -> TAXONOMIC_SCOPE.equals(o.getObjectClassificationLevel()))
        .findFirst()
        .ifPresent(v -> collection.setTaxonomicCoverage(v.getObjectClassificationName()));

    return collection;
  }

  public static Optional<UUID> getInstitutionKey(OrganisationalUnit organisationalUnit) {
    return organisationalUnit.getIdentifier().stream()
        .filter(i -> IdentifierTypes.INSTITUTION_GRSCICOLL_KEY.equals(i.getIdentifierType()))
        .map(Identifier::getIdentifierValue)
        .map(UUID::fromString)
        .findFirst();
  }

  public static Optional<UUID> getCollectionKey(ObjectGroup objectGroup) {
    return objectGroup.getIdentifier().stream()
        .filter(i -> IdentifierTypes.COLLECTION_GRSCICOLL_KEY.equals(i.getIdentifierType()))
        .map(Identifier::getIdentifierValue)
        .map(UUID::fromString)
        .findFirst();
  }

  private static Address toLatimerCoreAddress(
      org.gbif.api.model.collections.Address grscicollAddress, String type) {
    Address address = new Address();
    address.setStreetAddress(grscicollAddress.getAddress());
    address.setPostalCode(grscicollAddress.getPostalCode());
    address.setAddressLocality(grscicollAddress.getCity());
    address.setAddressRegion(grscicollAddress.getProvince());
    address.setAddressCountry(grscicollAddress.getCountry());
    address.setAddressType(type);
    return address;
  }

  private static org.gbif.api.model.collections.Address toCollectionsAddress(
      Address latimerAddress) {
    org.gbif.api.model.collections.Address address = new org.gbif.api.model.collections.Address();
    address.setAddress(latimerAddress.getStreetAddress());
    address.setPostalCode(latimerAddress.getPostalCode());
    address.setCity(latimerAddress.getAddressLocality());
    address.setProvince(latimerAddress.getAddressRegion());
    address.setCountry(latimerAddress.getAddressCountry());

    return address;
  }

  private static ContactDetail createContactDetail(String value, String category) {
    ContactDetail contactDetail = new ContactDetail();
    contactDetail.setContactDetailValue(value);
    contactDetail.setContactDetailCategory(category);
    return contactDetail;
  }

  private static Identifier createIdentifier(String value, String type) {
    Identifier identifier = new Identifier();
    identifier.setIdentifierType(type);
    identifier.setIdentifierValue(value);
    return identifier;
  }

  private static void addMeasurementOrFactText(
      String text, String type, List<MeasurementOrFact> measurementOrFactList) {
    if (!Strings.isNullOrEmpty(text)) {
      MeasurementOrFact measurementOrFact = new MeasurementOrFact();
      measurementOrFact.setMeasurementFactText(text);
      measurementOrFact.setMeasurementType(type);
      measurementOrFactList.add(measurementOrFact);
    }
  }

  private static void addMeasurementOrFactValue(
      Object value, String type, List<MeasurementOrFact> measurementOrFactList) {
    if (value != null && !value.toString().isEmpty()) {
      MeasurementOrFact measurementOrFact = new MeasurementOrFact();
      measurementOrFact.setMeasurementValue(value.toString());
      measurementOrFact.setMeasurementType(type);
      measurementOrFactList.add(measurementOrFact);
    }
  }

  private static void addReference(
      URI iri, String referenceName, String type, List<Reference> references) {
    if (iri != null) {
      Reference reference = new Reference();
      reference.setResourceIRI(iri);
      reference.setReferenceName(referenceName);
      reference.setReferenceType(type);
      references.add(reference);
    }
  }

  private static void addCollectionStatusHistory(
      String status, String type, ObjectGroup objectGroup) {
    if (!Strings.isNullOrEmpty(status)) {
      CollectionStatusHistory collectionStatusHistory = new CollectionStatusHistory();
      collectionStatusHistory.setStatus(status);
      collectionStatusHistory.setStatusType(type);
      objectGroup.getCollectionStatusHistory().add(collectionStatusHistory);
    }
  }
}
