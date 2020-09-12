/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.mail;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.config.MailConfigurationProperties;
import org.gbif.registry.mail.util.RegistryMailUtils;

import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Allows to send {@link BaseEmailModel}
 */
@Service
@Qualifier("emailSender")
public class EmailSenderImpl implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(EmailSenderImpl.class);

  private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

  private final JavaMailSender mailSender;
  private final MailConfigurationProperties mailConfigProperties;

  public EmailSenderImpl(
      JavaMailSender mailSender, MailConfigurationProperties mailConfigProperties) {
    this.mailSender = mailSender;
    this.mailConfigProperties = mailConfigProperties;
  }

  /**
   * Method that generates (using a template) and send an email containing a username and a
   * challenge code. This method will generate an HTML email.
   */
  @Override
  public void send(@Valid @NotNull BaseEmailModel emailModel) {
    RegistryMailUtils.toAddress(emailModel.getEmailAddress())
        .ifPresent(emailAddress -> prepareAndSend(emailModel, emailAddress));
  }

  private void prepareAndSend(BaseEmailModel emailModel, Address emailAddress) {
    try {
      // Send E-Mail
      final MimeMessage msg = mailSender.createMimeMessage();
      msg.setFrom(mailConfigProperties.getFrom());
      msg.setRecipient(Message.RecipientType.TO, emailAddress);
      msg.setRecipients(
          Message.RecipientType.CC,
          RegistryMailUtils.toInternetAddresses(emailModel.getCcAddress()).toArray(new Address[0]));
      msg.setRecipients(
          Message.RecipientType.BCC,
          RegistryMailUtils.toInternetAddresses(mailConfigProperties.getBcc())
              .toArray(new Address[0]));
      msg.setSubject(emailModel.getSubject());
      msg.setSentDate(new Date());

      MimeMultipart multipart = new MimeMultipart();

      BodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(emailModel.getBody(), HTML_CONTENT_TYPE);

      multipart.addBodyPart(htmlPart);

      BodyPart imagePart = new MimeBodyPart();
      imagePart.setDataHandler(new DataHandler(getImage()));
      imagePart.setHeader("Content-ID", "<image>");

      multipart.addBodyPart(imagePart);

      msg.setContent(multipart);

      if (mailConfigProperties.getEnabled() != null && mailConfigProperties.getEnabled()) {
        mailSender.send(msg);
      } else {
        LOG.warn("Mail sending is disabled!");
      }
    } catch (MessagingException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Sending of notification Mail for [{}] failed",
          emailAddress,
          e);
    }
  }

  private DataSource getImage() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = this.getClass().getClassLoader();
    }
    DataSource ds = new URLDataSource(classLoader.getResource("email/images/GBIF-2015-full.jpg"));
    return ds;
  }
}
