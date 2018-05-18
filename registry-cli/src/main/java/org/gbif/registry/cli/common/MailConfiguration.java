package org.gbif.registry.cli.common;

import java.util.Properties;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;

/**
 * Configuration for sending emails.
 */
@SuppressWarnings("PublicField")
public class MailConfiguration {

  private static final String MAIL_PREFIX = "mail.";

  @NotNull
  @Parameter(names = "--mail-devmail-enabled")
  public String devmailEnabled;

  @NotNull
  @Parameter(names = "--mail-smtp-host")
  public String smtpHost;

  @NotNull
  @Parameter(names = "--mail-smtp-port")
  public String smtpPort;

  @NotNull
  @Parameter(names = "--mail-devemail")
  public String devemail;

  @NotNull
  @Parameter(names = "--mail-cc")
  public String cc;

  @NotNull
  @Parameter(names = "--mail-from")
  public String from;

  public Properties toMailProperties() {
    Properties props = new Properties();

    props.put(MAIL_PREFIX + "devemail.enabled", this.devmailEnabled);
    props.put(MAIL_PREFIX + "smtp.host", this.smtpHost);
    props.put(MAIL_PREFIX + "smtp.port", this.smtpPort);
    props.put(MAIL_PREFIX + "devemail", this.devemail);
    props.put(MAIL_PREFIX + "cc", this.cc);
    props.put(MAIL_PREFIX + "from", this.from);

    return props;
  }

}
