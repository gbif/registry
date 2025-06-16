package org.gbif.registry.cli.vocabularysynchronizer;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

/** This command synchronizes vocabularies and runs post-processors. */
@MetaInfServices(Command.class)
public class VocabularySynchronizerCommand extends ServiceCommand {

  private final VocabularySynchronizerConfiguration config = new VocabularySynchronizerConfiguration();

  public VocabularySynchronizerCommand() {
    super("vocabulary-synchronizer");
  }

  @Override
  protected Service getService() {
    return new VocabularySynchronizerService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
} 