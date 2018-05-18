package org.gbif.registry.cli.gdprnotification;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.MailConfiguration;
import org.gbif.registry.gdpr.GdprConfiguration;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.base.Objects;

@SuppressWarnings("PublicField")
public class GdprNotificationConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration registry = new DbConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public MailConfiguration mailConfig = new MailConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public GdprConfiguration gdprConfig = new GdprConfiguration();

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("queueName", queueName)
      .add("messaging", messaging)
      .add("registry.serverName", registry.serverName)
      .add("registry.databaseName", registry.databaseName)
      .add("registry.user", registry.user)
      .add("registry.password", registry.password)
      .add("registry.maximumPoolSize", registry.maximumPoolSize)
      .add("registry.connectionTimeout", registry.connectionTimeout)
      .toString();
  }

  /**
   * Gdpr specific configuration.
   */
  public static class GdprConfiguration {

    private static final String URL_TEMPLATE_PROP_PREFIX = "urlTemplate.";
    private static final String GDPR_PREFIX = "gdpr.";
    private static final String MAIL_PREFIX = GDPR_PREFIX + "surety.mail.";

    @NotNull
    @Parameter(names = "--gdpr-version")
    public String gdprVersion;

    @Parameter(names = "--gdpr-mail-enabled")
    public boolean mailEnabled = false;

    @NotNull
    @Parameter(names = "--gdpr-subject")
    public String subject;

    @NotNull
    @Parameter(names = "--gdpr-infoPage")
    public String informationPage;

    @NotNull
    @Parameter(names = "--gdpr-node-urlTemplate")
    public String nodeUrlTemplate;

    @NotNull
    @Parameter(names = "--gdpr-organization-urlTemplate")
    public String organizationUrlTemplate;

    @NotNull
    @Parameter(names = "--gdpr-installation-urlTemplate")
    public String installationUrlTemplate;

    @NotNull
    @Parameter(names = "--gdpr-network-urlTemplate")
    public String networkUrlTemplate;

    @NotNull
    @Parameter(names = "--gdpr-dataset-urlTemplate")
    public String datasetUrlTemplate;

    public Properties toProperties() {
      Properties props = new Properties();

      props.put(GDPR_PREFIX + "version", this.gdprVersion);
      props.put(MAIL_PREFIX + "enabled", this.mailEnabled);
      props.put(MAIL_PREFIX + "subject", this.subject);
      props.put(MAIL_PREFIX + "informationPage", this.informationPage);
      props.put(MAIL_PREFIX + URL_TEMPLATE_PROP_PREFIX + "node", this.nodeUrlTemplate);
      props.put(MAIL_PREFIX + URL_TEMPLATE_PROP_PREFIX + "organization", this.organizationUrlTemplate);
      props.put(MAIL_PREFIX + URL_TEMPLATE_PROP_PREFIX + "installation", this.installationUrlTemplate);
      props.put(MAIL_PREFIX + URL_TEMPLATE_PROP_PREFIX + "network", this.networkUrlTemplate);
      props.put(MAIL_PREFIX + URL_TEMPLATE_PROP_PREFIX + "dataset", this.datasetUrlTemplate);

      return props;
    }
  }
}
