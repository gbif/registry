package org.gbif.identity.email;

import org.gbif.api.model.common.User;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.identity.util.LogUtils.NOTIFY_ADMIN;

import static freemarker.template.Configuration.VERSION_2_3_25;

/**
 *
 */
class IdentityEmailManagerImpl implements IdentityEmailManager {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityEmailManagerImpl.class);
  private static final Splitter EMAIL_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();

  static final String USER_CREATE_TEMPLATE = "create_confirmation_en.ftl";
  static final String RESET_PASSWORD_TEMPLATE = "reset_password_en.ftl";

  private static final Configuration FREEMARKER_CONFIG = new Configuration(VERSION_2_3_25);
  static {
    FREEMARKER_CONFIG.setDefaultEncoding("UTF-8");
    FREEMARKER_CONFIG.setLocale(Locale.US);
    FREEMARKER_CONFIG.setNumberFormat("0.####");
    FREEMARKER_CONFIG.setDateFormat("yyyy-mm-dd");
    // create custom rendering for relative dates
    //FREEMARKER_CONFIG.setSharedVariable("niceDate", new NiceDateTemplateMethodModel());
    FREEMARKER_CONFIG.setClassForTemplateLoading(IdentityEmailManagerImpl.class, "/email");
  }

  private final Session session;
  private final Set<Address> bccAddresses;

  @Inject
  IdentityEmailManagerImpl(Session session, @Nullable @Named("bcc") String bccAddresses) {
    this.session = session;
    this.bccAddresses = bccAddresses != null ?
            Sets.newHashSet(toInternetAddresses(EMAIL_SPLITTER.split(bccAddresses))) : Collections.emptySet();
  }

  @Override
  public void generateAndSendUserCreated(User user, UUID challengeCode) {
    //generate URL with challengeCode;
    BaseEmailModel emailModel = new BaseEmailModel(user.getUserName(), null);

    Optional<Address> emailAddress = toAddress(user.getEmail());
    if(emailAddress.isPresent()) {
      try {
        sendMail(emailAddress.get(), "Account creation on gbif.org", buildEmailBody(emailModel, USER_CREATE_TEMPLATE));
      } catch (IOException | TemplateException e) {
        LOG.error(NOTIFY_ADMIN, "Rendering of notification Mail for [{}] failed", emailAddress, e);
      }
    }
  }

  @Override
  public void generateAndSendPasswordReset(User user, UUID challengeCode) {

  }

  @VisibleForTesting
  protected String buildEmailBody(BaseEmailModel emailModel, String templateFile) throws IOException, TemplateException {
    // Prepare the E-Mail body text
    StringWriter contentBuffer = new StringWriter();
    Template template = FREEMARKER_CONFIG.getTemplate(templateFile);
    template.process(emailModel, contentBuffer);
    return contentBuffer.toString();
  }

  /**
   * Utility method that sends a notification email.
   */
  private void sendMail(Address emailAddress, String subject, String body) {
    Objects.requireNonNull(emailAddress, "emailAddress shall be provided");

    if (emailAddress == null || bccAddresses.isEmpty()) {
      LOG.warn("No valid notification addresses given");
      return;
    }
    try {
      // Send E-Mail
      MimeMessage msg = new MimeMessage(session);
      msg.setFrom();
      msg.setRecipient(Message.RecipientType.TO, emailAddress);
      msg.setRecipients(Message.RecipientType.BCC, bccAddresses.toArray(new Address[bccAddresses.size()]));
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      msg.setText(body);
      Transport.send(msg);
    } catch (MessagingException e) {
      LOG.error(NOTIFY_ADMIN, "Sending of notification Mail for [{}] failed", emailAddress, e);
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
