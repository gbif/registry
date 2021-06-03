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

import org.gbif.registry.mail.config.MailConfigurationProperties;
import org.gbif.registry.mail.util.RegistryMailUtils;

import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Allows to send {@link BaseEmailModel} */
@Service
@Qualifier("emailSender")
public class EmailSenderImpl implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(EmailSenderImpl.class);

  private final JavaMailSender mailSender;
  private final MailConfigurationProperties mailConfigProperties;

  @Value("classpath:email/images/GBIF-logo.png")
  private Resource logoFile;

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
  public void send(BaseEmailModel emailModel) {
    if (emailModel == null) {
      LOG.warn("Email model is null, skip email sending");
      return;
    }

    if (emailModel.getEmailAddresses().isEmpty()) {
      LOG.warn("No valid email addresses");
      return;
    }

    prepareAndSend(emailModel);
  }

  private void prepareAndSend(BaseEmailModel emailModel) {
    try {
      // Send E-Mail
      final MimeMessage msg = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

      if (mailConfigProperties.getDevemail().getEnabled()) {
        helper.setTo(mailConfigProperties.getDevemail().getAddress());
      } else {
        helper.setTo(emailModel.getEmailAddresses().toArray(new String[0]));
        helper.setCc(emailModel.getCcAddresses().toArray(new String[0]));
        helper.setBcc(mailConfigProperties.getBcc().toArray(new String[0]));
      }

      if (emailModel.getFrom() != null) {
        helper.setFrom(emailModel.getFrom());
      } else {
        helper.setFrom(mailConfigProperties.getFrom());
      }

      helper.setSubject(emailModel.getSubject());
      helper.setSentDate(new Date());
      helper.setText(emailModel.getBody(), true);
      helper.addInline("logo.png", logoFile);

      mailSender.send(msg);
    } catch (MessagingException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Sending of notification Mail for [{}] failed",
          emailModel.getEmailAddresses(),
          e);
    }
  }
}
