package org.gbif.registry.surety.email;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class RegistryEmailUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryEmailUtils.class);

  private static final Splitter EMAIL_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();

  private RegistryEmailUtils() {
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
  public static Set<Address> toInternetAddresses(String strEmails) {
    return StreamSupport.stream(EMAIL_SPLITTER.split(strEmails).spliterator(), false)
        .map(RegistryEmailUtils::toAddress)
        .flatMap(address -> address.map(Stream::of).orElseGet(Stream::empty))
        .collect(Collectors.toSet());
  }

  /**
   * Join email addresses from config and model.
   */
  public static Address[] getUnitedBccArray(Set<Address> bccAddressesFromConfig, BaseEmailModel emailModel) {
    Set<Address> combinedBccAddresses = new HashSet<>(bccAddressesFromConfig);
    Optional.ofNullable(emailModel.getCcAddress())
        .ifPresent( bccList -> bccList.forEach( bcc -> RegistryEmailUtils.toAddress(bcc).ifPresent(combinedBccAddresses::add)));
    return combinedBccAddresses.toArray(new Address[combinedBccAddresses.size()]);
  }
}
