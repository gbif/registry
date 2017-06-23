package org.gbif.registry.surety.email;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Allows to send {@link BaseEmailModel}
 */
class EmailManagerImpl implements EmailManager {

  public static final Marker NOTIFY_ADMIN = MarkerFactory.getMarker("NOTIFY_ADMIN");

  private static final Logger LOG = LoggerFactory.getLogger(EmailManagerImpl.class);
  private static final Splitter EMAIL_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();

  private final Session session;
  private final Set<Address> bccAddresses;

  @Inject
  EmailManagerImpl(EmailManagerConfiguration config) {
    this.session = config.getSession();
    this.bccAddresses = config.getBccAddresses() != null ?
            Sets.newHashSet(toInternetAddresses(EMAIL_SPLITTER.split(config.getBccAddresses()))) : Collections.emptySet();
  }

  /**
   * Method that generates (using a template) and send an email containing an username and a challenge code.
   */
  @Override
  public void send(BaseEmailModel emailModel) {

    Objects.requireNonNull(emailModel, "emailModel shall be provided");
    Objects.requireNonNull(emailModel.getEmailAddress(), "emailAddress shall be provided");

    Optional<Address> emailAddress = toAddress(emailModel.getEmailAddress());
    if (emailAddress.isPresent()) {
      try {
        // Send E-Mail
        MimeMessage msg = new MimeMessage(session);
        //from will be set with the value from the {@link Session} object.
        msg.setFrom();
        msg.setRecipient(Message.RecipientType.TO, emailAddress.get());
        msg.setRecipients(Message.RecipientType.BCC, bccAddresses.toArray(new Address[bccAddresses.size()]));
        msg.setSubject(emailModel.getSubject());
        msg.setSentDate(new Date());
        msg.setText(emailModel.getBody());
        Transport.send(msg);
      } catch (MessagingException e) {
        LOG.error(NOTIFY_ADMIN, "Sending of notification Mail for [{}] failed", emailAddress, e);
      }
    }
  }

  /**
   * Transforms a iterable of string into a list of email addresses.
   */
  private static List<Address> toInternetAddresses(Iterable<String> strEmails) {
    List<Address> emails = Lists.newArrayList();
    for (String address : strEmails) {
      try {
        emails.add(new InternetAddress(address));
      } catch (AddressException e) {
        // bad address?
        LOG.warn("Ignore corrupt email address {}", address);
      }
    }
    return emails;
  }

  private static Optional<Address> toAddress(String emailAddress) {
    try {
      return Optional.of(new InternetAddress(emailAddress));
    } catch (AddressException e) {
      // bad address?
      LOG.warn("Ignore corrupt email address {}", emailAddress);
    }
    return Optional.empty();
  }
}
