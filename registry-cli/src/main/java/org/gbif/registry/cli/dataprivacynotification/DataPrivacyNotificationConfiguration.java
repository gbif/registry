package org.gbif.registry.cli.dataprivacynotification;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.MailConfiguration;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.base.Objects;

@SuppressWarnings("PublicField")
public class DataPrivacyNotificationConfiguration {

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
  public DataPrivacyConfiguration dataPrivacyConfig = new DataPrivacyConfiguration();

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
   * Data privacy specific configuration.
   */
  public static class DataPrivacyConfiguration {

    private static final String URL_TEMPLATE_PROP_PREFIX = "urlTemplate.";
    private static final String DATA_PRIVACY_PREFIX = "dataPrivacy.";
    private static final String MAIL_PREFIX = DATA_PRIVACY_PREFIX + "surety.mail.";

    @NotNull
    @Parameter(names = "--dataPrivacy-version")
    public String dataPrivacyVersion;

    @Parameter(names = "--dataPrivacy-mail-enabled")
    public boolean mailEnabled = false;

    @NotNull
    @Parameter(names = "--dataPrivacy-subject")
    public String subject;

    @NotNull
    @Parameter(names = "--dataPrivacy-infoPage")
    public String informationPage;

    @NotNull
    @Parameter(names = "--dataPrivacy-node-urlTemplate")
    public String nodeUrlTemplate;

    @NotNull
    @Parameter(names = "--dataPrivacy-organization-urlTemplate")
    public String organizationUrlTemplate;

    @NotNull
    @Parameter(names = "--dataPrivacy-installation-urlTemplate")
    public String installationUrlTemplate;

    @NotNull
    @Parameter(names = "--dataPrivacy-network-urlTemplate")
    public String networkUrlTemplate;

    @NotNull
    @Parameter(names = "--dataPrivacy-dataset-urlTemplate")
    public String datasetUrlTemplate;

    public Properties toProperties() {
      Properties props = new Properties();

      props.put(DATA_PRIVACY_PREFIX + "version", this.dataPrivacyVersion);
      props.put(MAIL_PREFIX + "enabled", String.valueOf(this.mailEnabled));
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
