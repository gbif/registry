package org.gbif.registry.cli.gdprnotification;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class GdprNotificationCommand extends ServiceCommand {

  private final GdprNotificationConfiguration configuration = new GdprNotificationConfiguration();

  public GdprNotificationCommand() {
    super("gdpr-notification");
  }

  @Override
  protected Service getService() {
    return new GdprNotificationService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }
}
