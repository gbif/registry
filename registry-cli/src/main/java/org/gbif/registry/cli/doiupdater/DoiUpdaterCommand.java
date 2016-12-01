package org.gbif.registry.cli.doiupdater;


import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class DoiUpdaterCommand extends ServiceCommand {

  private final DoiUpdaterConfiguration config = new DoiUpdaterConfiguration();

  public DoiUpdaterCommand() {
    super("doi-updater");
  }

  @Override
  protected Service getService() {
    return new DoiUpdaterService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
}
