package org.gbif.registry.surety.email;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.surety.SuretyConstants.NOTIFY_ADMIN;

/**
 * Allows to send {@link BaseEmailModel}
 */
class EmailManagerImpl implements EmailManager {

  private static final Logger LOG = LoggerFactory.getLogger(EmailManagerImpl.class);
  private static final Splitter EMAIL_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();

  private final Session session;
  private final Set<Address> bccAddresses;

  @Inject
  EmailManagerImpl(EmailManagerConfiguration config) {
    session = config.getSession();
    bccAddresses = Optional.ofNullable(config.getBccAddresses())
                    .map(bccAddresses -> toInternetAddresses(EMAIL_SPLITTER.split(bccAddresses)))
                    .orElse(Collections.emptySet());
  }

  /**
   * Method that generates (using a template) and send an email containing an username and a challenge code.
   */
  @Override
  public void send(BaseEmailModel emailModel) {

    Objects.requireNonNull(emailModel, "emailModel shall be provided");
    Objects.requireNonNull(emailModel.getEmailAddress(), "emailAddress shall be provided");

    toAddress(emailModel.getEmailAddress())
      .ifPresent(emailAddress -> {
            try {
              // Send E-Mail
              MimeMessage msg = new MimeMessage(session);
              //from will be set with the value from the {@link Session} object.
              msg.setFrom();
              msg.setRecipient(Message.RecipientType.TO, emailAddress);
              msg.setRecipients(Message.RecipientType.BCC, generateBccArray(bccAddresses, emailModel));
              msg.setSubject(emailModel.getSubject());
              msg.setSentDate(new Date());
              msg.setText(emailModel.getBody());
              Transport.send(msg);
            } catch (MessagingException e) {
              LOG.error(NOTIFY_ADMIN, "Sending of notification Mail for [{}] failed", emailAddress, e);
            }
          }
      );
  }

  /**
   * Transforms a iterable of string into a list of email addresses.
   */
  private static Set<Address> toInternetAddresses(Iterable<String> strEmails) {
    return StreamSupport.stream(strEmails.spliterator(), false)
            .map(EmailManagerImpl::toAddress)
            .flatMap(address -> address.map(Stream::of).orElseGet(Stream::empty))
            .collect(Collectors.toSet());
  }

  @VisibleForTesting
  static Address[] generateBccArray(Set<Address> bccAddressesFromConfig, BaseEmailModel emailModel) {
    Set<Address> combinedBccAddresses = new HashSet<>(bccAddressesFromConfig);
    Optional.ofNullable(emailModel.getCcAddress())
            .ifPresent( bccList -> bccList.forEach( bcc -> toAddress(bcc).ifPresent(combinedBccAddresses::add)));
    return combinedBccAddresses.toArray(new Address[combinedBccAddresses.size()]);
  }

  @VisibleForTesting
  static Optional<Address> toAddress(String emailAddress) {
    try {
      return Optional.of(new InternetAddress(emailAddress));
    } catch (AddressException e) {
      // bad address?
      LOG.warn("Ignore corrupt email address {}", emailAddress);
    }
    return Optional.empty();
  }
}
