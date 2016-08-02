package org.gbif.registry.cli.doisynchronizer;

import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import org.kohsuke.MetaInfServices;


@MetaInfServices(Command.class)
public class DoiSynchronizerCommand extends BaseCommand {

  private final DoiSynchronizerConfiguration config = new DoiSynchronizerConfiguration();


  public DoiSynchronizerCommand() {
    super("doi-synchronizer");
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }


  @Override
  protected void doRun() {
    DoiSynchronizerService service = new DoiSynchronizerService(config);
    service.doRun();
  }

}
