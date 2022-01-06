package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.service.collections.utils.IdentifierValidatorUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConverterUtils {

  private static final Pattern COMMA_COLON = Pattern.compile(",:");
  private static final Pattern COMMA_SEMICOLON = Pattern.compile(",;");
  private static final Pattern DOT_COLON = Pattern.compile("\\.:");
  private static final Pattern DOT_SEMICOLON = Pattern.compile("\\.;");
  private static final Pattern MULTIPLE_COMMAS = Pattern.compile(",{2,}");
  private static final Pattern MULTIPLE_DOTS = Pattern.compile("\\.{2,}");

  public static String normalizePunctuationSigns(String s) {
    s = COMMA_COLON.matcher(s).replaceAll(":");
    s = COMMA_SEMICOLON.matcher(s).replaceAll(";");
    s = DOT_COLON.matcher(s).replaceAll(":");
    s = DOT_SEMICOLON.matcher(s).replaceAll(";");
    s = MULTIPLE_COMMAS.matcher(s).replaceAll(",");
    s = MULTIPLE_DOTS.matcher(s).replaceAll(".");
    return s;
  }

  public static Contact datasetContactToCollectionsContact(
      org.gbif.api.model.registry.Contact datasetContact) {
    Contact collectionContact = new Contact();
    collectionContact.setFirstName(datasetContact.getFirstName());
    collectionContact.setLastName(datasetContact.getLastName());
    collectionContact.setPrimary(datasetContact.isPrimary());

    if (datasetContact.getUserId() != null && !datasetContact.getUserId().isEmpty()) {
      List<UserId> userIds =
          datasetContact.getUserId().stream()
              .map(id -> new UserId(IdentifierValidatorUtils.getIdType(id), id))
              .collect(Collectors.toList());
      collectionContact.setUserIds(userIds);
    }

    collectionContact.setPosition(datasetContact.getPosition());
    collectionContact.setEmail(datasetContact.getEmail());
    collectionContact.setPhone(datasetContact.getPhone());
    collectionContact.setAddress(datasetContact.getAddress());
    collectionContact.setCity(datasetContact.getCity());
    collectionContact.setProvince(datasetContact.getProvince());
    collectionContact.setCountry(datasetContact.getCountry());
    collectionContact.setPostalCode(datasetContact.getPostalCode());
    return collectionContact;
  }

  public static Address convertAddress(Organization organization, Address existingAddress) {
    if (existingAddress == null) {
      existingAddress = new Address();
    }

    if (organization.getAddress() != null && !organization.getAddress().isEmpty()) {
      String address = String.join(".", organization.getAddress());
      address = MULTIPLE_DOTS.matcher(address).replaceAll(".");
      existingAddress.setAddress(address);
    }
    existingAddress.setCity(organization.getCity());
    existingAddress.setProvince(organization.getProvince());
    existingAddress.setPostalCode(organization.getPostalCode());
    existingAddress.setCountry(organization.getCountry());

    // if all fields are null we return null
    Address emptyAddress = new Address();
    if (existingAddress.lenientEquals(emptyAddress)) {
      return null;
    }

    return existingAddress;
  }
}
