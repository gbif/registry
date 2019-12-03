package org.gbif.registry.mail;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.util.RegistryMailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * Allows to send {@link BaseEmailModel}
 */
@Service
@Qualifier("emailSender")
class EmailSenderImpl implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(EmailSenderImpl.class);

  private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

  @Value("${mail.bcc}")
  private String bcc;

  private final JavaMailSender mailSender;

  public EmailSenderImpl(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Method that generates (using a template) and send an email containing a username and a challenge code.
   * This method will generate an HTML email.
   */
  @Override
  public void send(@Valid @NotNull BaseEmailModel emailModel) {
    RegistryMailUtils.toAddress(emailModel.getEmailAddress())
      .ifPresent(emailAddress -> {
          try {
            // Send E-Mail
            final MimeMessage msg = mailSender.createMimeMessage();
            // from will be set with the value from the {@link Session} object.
            msg.setFrom();
            msg.setRecipient(Message.RecipientType.TO, emailAddress);
            msg.setRecipients(Message.RecipientType.BCC, RegistryMailUtils.getUnitedBccArray(getProcessedBccAddresses(), emailModel));
            msg.setSubject(emailModel.getSubject());
            msg.setSentDate(new Date());
            msg.setContent(emailModel.getBody(), HTML_CONTENT_TYPE);
            mailSender.send(msg);
          } catch (MessagingException e) {
            LOG.error(RegistryMailUtils.NOTIFY_ADMIN, "Sending of notification Mail for [{}] failed", emailAddress, e);
          }
        }
      );
  }

  private Set<Address> getProcessedBccAddresses() {
    return Optional.ofNullable(bcc)
      .map(RegistryMailUtils::toInternetAddresses)
      .orElse(Collections.emptySet());
  }
}
