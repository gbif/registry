package org.gbif.identity.email;

import org.gbif.api.model.common.User;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.validation.constraints.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.identity.email.IdentityEmailManagerConfiguration.FREEMARKER_TEMPLATES_LOCATION;
import static org.gbif.identity.email.IdentityEmailManagerConfiguration.RESET_PASSWORD_SUBJECT_KEY;
import static org.gbif.identity.email.IdentityEmailManagerConfiguration.RESET_PASSWORD_TEMPLATE;
import static org.gbif.identity.email.IdentityEmailManagerConfiguration.USER_CREATE_SUBJECT_KEY;
import static org.gbif.identity.email.IdentityEmailManagerConfiguration.USER_CREATE_TEMPLATE;
import static org.gbif.identity.util.LogUtils.NOTIFY_ADMIN;

import static freemarker.template.Configuration.VERSION_2_3_25;

/**
 *
 */
class IdentityEmailManagerImpl implements IdentityEmailManager {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityEmailManagerImpl.class);
  private static final Splitter EMAIL_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();

  private static final Configuration FREEMARKER_CONFIG = new Configuration(VERSION_2_3_25);
  static {
    FREEMARKER_CONFIG.setDefaultEncoding("UTF-8");
    FREEMARKER_CONFIG.setLocale(Locale.US);
    FREEMARKER_CONFIG.setNumberFormat("0.####");
    FREEMARKER_CONFIG.setDateFormat("yyyy-mm-dd");
    // create custom rendering for relative dates
    //FREEMARKER_CONFIG.setSharedVariable("niceDate", new NiceDateTemplateMethodModel());
    FREEMARKER_CONFIG.setClassForTemplateLoading(IdentityEmailManagerImpl.class, FREEMARKER_TEMPLATES_LOCATION);
  }

  private final Session session;
  private final Set<Address> bccAddresses;
  private final String confirmUrlTemplate;
  private final String resetPasswordUrlTemplate;

  //English email subjects
  private final ResourceBundle defaultEmailSubjects;

  @Inject
  IdentityEmailManagerImpl(IdentityEmailManagerConfiguration config) {
    this.session = config.getSession();
    this.bccAddresses = config.getBccAddresses() != null ?
            Sets.newHashSet(toInternetAddresses(EMAIL_SPLITTER.split(config.getBccAddresses()))) : Collections.emptySet();
    this.confirmUrlTemplate = config.getConfirmUrlTemplate();
    this.resetPasswordUrlTemplate = config.getResetPasswordUrlTemplate();

    defaultEmailSubjects = config.getDefaultEmailSubjects();
  }

  @Override
  public void generateAndSendUserCreated(User user, UUID challengeCode) {
    generateAndSend(user, confirmUrlTemplate, challengeCode, USER_CREATE_SUBJECT_KEY, USER_CREATE_TEMPLATE);
  }

  @Override
  public void generateAndSendPasswordReset(User user, UUID challengeCode) {
    generateAndSend(user, resetPasswordUrlTemplate, challengeCode, RESET_PASSWORD_SUBJECT_KEY, RESET_PASSWORD_TEMPLATE);
  }

  /**
   * Method that generates (using a template) and send an email containing an username and a challenge code.
   *
   * @param user
   * @param urlTemplate
   * @param challengeCode
   * @param subjectKey
   * @param template      template to use to generate the body of the email
   */
  private void generateAndSend(User user, String urlTemplate, UUID challengeCode,
                               String subjectKey, String template) {
    //generate URL with challengeCode;
    BaseEmailModel emailModel = null;
    try {
      emailModel = new BaseEmailModel(user.getUserName(),
              new URL(MessageFormat.format(urlTemplate, user.getUserName(), challengeCode)));
    } catch (MalformedURLException e) {
      LOG.error(NOTIFY_ADMIN, "Rendering of notification Mail for [{}] failed", user.getUserName(), e);
    }
    //make it final for the lambda
    final BaseEmailModel finalEmailModel = emailModel;
    toAddress(user.getEmail()).ifPresent(
            email -> {
              try {
                sendMail(email, defaultEmailSubjects.getString(subjectKey),
                        buildEmailBody(finalEmailModel, template));
              } catch (IOException | TemplateException e) {
                LOG.error(NOTIFY_ADMIN, "Rendering of notification Mail for [{}] failed", email, e);
              }
            });
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
  private void sendMail(@NotNull Address emailAddress, String subject, String body) {
    Objects.requireNonNull(emailAddress, "emailAddress shall be provided");

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
