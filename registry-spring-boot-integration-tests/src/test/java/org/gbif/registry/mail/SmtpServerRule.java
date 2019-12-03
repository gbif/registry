package org.gbif.registry.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.rules.ExternalResource;

import javax.mail.internet.MimeMessage;

public class SmtpServerRule extends ExternalResource {

  private GreenMail smtpServer;
  private int port;

  public SmtpServerRule(int port) {
    this.port = port;
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    smtpServer = new GreenMail(new ServerSetup(port, null, "smtp"));
    smtpServer.start();
  }

  public MimeMessage[] getMessages() {
    return smtpServer.getReceivedMessages();
  }

  public Retriever getRetriever() {
    return new Retriever(smtpServer.getPop3());
  }

  @Override
  protected void after() {
    super.after();
    smtpServer.stop();
  }
}
