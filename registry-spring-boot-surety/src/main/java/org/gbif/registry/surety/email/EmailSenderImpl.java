package org.gbif.registry.surety.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
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

import static org.gbif.registry.surety.SuretyConstants.NOTIFY_ADMIN;
import static org.gbif.registry.surety.email.RegistryEmailUtils.getUnitedBccArray;
import static org.gbif.registry.surety.email.RegistryEmailUtils.toAddress;

/**
 * Allows to send {@link BaseEmailModel}
 */
@Service
@Primary
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
    toAddress(emailModel.getEmailAddress())
      .ifPresent(emailAddress -> {
            try {
              // Send E-Mail
              final MimeMessage msg = mailSender.createMimeMessage();
              //from will be set with the value from the {@link Session} object.
              msg.setFrom();
              // TODO: 2019-06-27 ask why cc and bcc squashed
              msg.setRecipient(Message.RecipientType.TO, emailAddress);
//              msg.setRecipient(Message.RecipientType.CC, emailModel);
              msg.setRecipients(Message.RecipientType.BCC, getUnitedBccArray(getProcessedBccAddresses(), emailModel));
              msg.setSubject(emailModel.getSubject());
              msg.setSentDate(new Date());
              msg.setContent(emailModel.getBody(), HTML_CONTENT_TYPE);
              mailSender.send(msg);
            } catch (MessagingException e) {
              LOG.error(NOTIFY_ADMIN, "Sending of notification Mail for [{}] failed", emailAddress, e);
            }
          }
      );
  }

  Set<Address> getProcessedBccAddresses() {
    return Optional.ofNullable(bcc)
        .map(RegistryEmailUtils::toInternetAddresses)
        .orElse(Collections.emptySet());
  }
}
