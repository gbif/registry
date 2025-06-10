package org.gbif.registry.cli.vocabularyfacetupdater;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

/** This command updates vocabulary facets table. */
@MetaInfServices(Command.class)
public class VocabularyFacetUpdaterCommand extends ServiceCommand {

  private final VocabularyFacetUpdaterConfiguration config = new VocabularyFacetUpdaterConfiguration();

  public VocabularyFacetUpdaterCommand() {
    super("vocabulary-facet-updater");
  }

  @Override
  protected Service getService() {
    return new VocabularyFacetUpdaterService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
}
