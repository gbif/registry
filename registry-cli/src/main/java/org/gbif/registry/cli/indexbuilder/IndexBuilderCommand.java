package org.gbif.registry.cli.indexbuilder;

import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.registry.cli.common.stubs.DoiGeneratorStub;
import org.gbif.registry.cli.common.stubs.DoiHandlerStrategyStub;
import org.gbif.registry.cli.common.stubs.EditorAuthorizationServiceStub;
import org.gbif.registry.directory.DirectoryModule;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.search.util.DatasetIndexBuilder;
import org.gbif.registry.ws.security.EditorAuthorizationService;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@MetaInfServices(Command.class)
public class IndexBuilderCommand extends BaseCommand {
  private static final Logger LOG = LoggerFactory.getLogger(IndexBuilderCommand.class);

  private final IndexBuilderConfig cfg;

  public IndexBuilderCommand() {
    this(new IndexBuilderConfig());
  }

  public IndexBuilderCommand(IndexBuilderConfig cfg) {
    super("index-builder");
    this.cfg = cfg;
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  @Override
  protected void doRun() {
    try {
      Properties props = cfg.toProperties();
      Injector inj = Guice.createInjector(
          new RegistryMyBatisModule(props),
          new RegistrySearchModule(props),
          new DirectoryModule(props),
          new StubModule(),
          EventModule.withoutRabbit(props)
      );

      DatasetIndexBuilder idxBuilder = inj.getInstance(DatasetIndexBuilder.class);
      LOG.info("Building new {} solr index for collection {} on {}", cfg.solr.serverType, cfg.solr.collection, cfg.solr.serverHome);
      idxBuilder.build();

    } catch (Exception e) {
      LOG.error("Failed to run index builder", e);
    }

  }

  static class StubModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(DoiGenerator.class).to(DoiGeneratorStub.class);
      bind(DataCiteDoiHandlerStrategy.class).to(DoiHandlerStrategyStub.class);
      bind(EditorAuthorizationService.class).to(EditorAuthorizationServiceStub.class);
    }
  }

}
