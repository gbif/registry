package org.gbif.registry.surety.email;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: 2019-08-15 fix
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@Ignore
public class EmailSenderIT {

  @Autowired
  private EmailSender emailService;

  @Rule
  public SmtpServerRule smtpServerRule = new SmtpServerRule(2525);

  @Test
  public void testSendWhenTwoValidCcEmailThenGotFourMessages() throws MessagingException, IOException {
    BaseEmailModel mail = new BaseEmailModel(
      "info@memorynotfound.com",
      "Spring Mail Integration Testing with JUnit and GreenMail Example",
      "We show how to write Integration Tests using Spring and GreenMail.",
      Arrays.asList("cc1", "cc2")
    );

    emailService.send(mail);

    MimeMessage[] receivedMessages = smtpServerRule.getMessages();

    // primary email, two from model cc list and one from property file
    assertEquals(4, receivedMessages.length);

    MimeMessage current = receivedMessages[0];

    assertEquals(mail.getSubject(), current.getSubject());
    assertEquals(mail.getEmailAddress(), current.getAllRecipients()[0].toString());
    assertEquals(mail.getBody(), current.getContent().toString().trim());
    assertTrue(String.valueOf(current.getContent()).contains(mail.getBody()));
  }

  @Test
  public void testSendWhenOneCcEmailWrongThenGotTwoMessages() throws Exception {

    BaseEmailModel mail = new BaseEmailModel(
      "info@memorynotfound.com",
      "Spring Mail Integration Testing with JUnit and GreenMail Example",
      "We show how to write Integration Tests using Spring and GreenMail.",
      Collections.singletonList("wrong email")
    );

    emailService.send(mail);

    MimeMessage[] receivedMessages = smtpServerRule.getMessages();

    // primary email and one from property file
    assertEquals(2, receivedMessages.length);

    MimeMessage current = receivedMessages[0];

    assertEquals(mail.getSubject(), current.getSubject());
    assertEquals(mail.getEmailAddress(), current.getAllRecipients()[0].toString());
    assertEquals(mail.getBody(), current.getContent().toString().trim());
    assertTrue(String.valueOf(current.getContent()).contains(mail.getBody()));
  }
}
