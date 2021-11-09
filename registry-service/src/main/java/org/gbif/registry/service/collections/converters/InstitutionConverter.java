package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceType;
import org.gbif.api.model.registry.Organization;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.registry.service.collections.converters.ConverterUtils.convertAddress;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstitutionConverter {

  public static Institution convertFromOrganization(
      Organization organization, String institutionCode) {
    Objects.requireNonNull(organization);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(institutionCode));

    Institution institution = new Institution();
    institution.setCode(institutionCode);
    institution.setName(organization.getTitle());
    institution.setDescription(organization.getDescription());
    institution.setMasterSource(MasterSourceType.GBIF_REGISTRY);

    if (organization.getHomepage() != null && !organization.getHomepage().isEmpty()) {
      institution.setHomepage(organization.getHomepage().get(0));
    }

    institution.setPhone(organization.getPhone());
    institution.setEmail(organization.getEmail());
    institution.setLatitude(organization.getLatitude());
    institution.setLongitude(organization.getLongitude());
    institution.setLogoUrl(organization.getLogoUrl());
    institution.setActive(true);

    institution.setAddress(convertAddress(organization));

    // contacts
    List<Contact> collectionContacts =
        organization.getContacts().stream()
            .map(ConverterUtils::datasetContactToCollectionsContact)
            .collect(Collectors.toList());
    institution.setContactPersons(collectionContacts);

    return institution;
  }
}
