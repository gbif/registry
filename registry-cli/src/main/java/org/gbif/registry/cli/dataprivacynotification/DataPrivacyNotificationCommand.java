package org.gbif.registry.cli.dataprivacynotification;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class DataPrivacyNotificationCommand extends ServiceCommand {

  private final DataPrivacyNotificationConfiguration configuration = new DataPrivacyNotificationConfiguration();

  public DataPrivacyNotificationCommand() {
    super("data-privacy-notification");
  }

  @Override
  protected Service getService() {
    return new DataPrivacyNotificationService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }
}
