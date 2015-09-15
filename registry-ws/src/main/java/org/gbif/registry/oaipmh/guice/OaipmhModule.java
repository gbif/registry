package org.gbif.registry.oaipmh.guice;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;
import org.gbif.registry.search.DatasetSearchServiceSolr;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;
import com.lyncode.xoai.dataprovider.repository.SetRepository;

/**
 * A guice module that sets up implementation of XOAI related classes.
 *
 * @author cgendreau
 */
public class OaipmhModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
  }
}
