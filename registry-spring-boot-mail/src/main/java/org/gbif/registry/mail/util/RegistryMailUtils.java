package org.gbif.registry.mail.util;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegistryMailUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryMailUtils.class);

  public static final Marker NOTIFY_ADMIN = MarkerFactory.getMarker("NOTIFY_ADMIN");

  private RegistryMailUtils() {
  }

  /**
   * Transforms a string email address to {@link Address}.
   */
  public static Optional<Address> toAddress(String emailAddress) {
    try {
      return Optional.of(new InternetAddress(emailAddress));
    } catch (AddressException e) {
      // bad address?
      LOG.warn("Ignore corrupt email address {}", emailAddress);
    }
    return Optional.empty();
  }

  /**
   * Transforms a string of addresses into a list of email addresses.
   */
  public static Set<Address> toInternetAddresses(List<String> strEmails) {
    return strEmails.stream()
      .map(RegistryMailUtils::toAddress)
      .flatMap(address -> address.map(Stream::of).orElseGet(Stream::empty))
      .collect(Collectors.toSet());
  }

  /**
   * Join email addresses from config and model.
   */
  public static Address[] getUnitedBccArray(Set<Address> bccAddressesFromConfig, BaseEmailModel emailModel) {
    Set<Address> combinedBccAddresses = new HashSet<>(bccAddressesFromConfig);
    Optional.ofNullable(emailModel.getCcAddress())
      .ifPresent(ccList -> ccList.forEach(cc -> RegistryMailUtils.toAddress(cc).ifPresent(combinedBccAddresses::add)));
    return combinedBccAddresses.toArray(new Address[0]);
  }
}
