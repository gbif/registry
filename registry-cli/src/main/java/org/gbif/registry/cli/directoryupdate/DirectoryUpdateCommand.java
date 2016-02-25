package org.gbif.registry.cli.directoryupdate;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

/**
 *
 */
@MetaInfServices(Command.class)
public class DirectoryUpdateCommand extends ServiceCommand {

    private final DirectoryUpdateConfiguration config = new DirectoryUpdateConfiguration();

    public DirectoryUpdateCommand() {
      super("directory-update");
    }

    @Override
    protected Service getService() {
      return new DirectoryUpdateService(config);
    }

    @Override
    protected Object getConfigurationObject() {
      return config;
    }

}
