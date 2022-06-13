package org.gbif.registry.cli.datasetindex.indexupdater;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

import com.google.common.util.concurrent.Service;
/** This command updates either a single dataset or a list of datasets in the ES index. */
@MetaInfServices(Command.class)
public class DatasetIndexUpdaterCommand extends ServiceCommand {

  private final DatasetIndexUpdaterConfiguration config = new DatasetIndexUpdaterConfiguration();

  public DatasetIndexUpdaterCommand() {
    super("dataset-index-updater");
  }

  @Override
  protected Service getService() {
    return new DatasetIndexUpdaterService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
}
