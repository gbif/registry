package org.gbif.registry.search;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.search.util.TimeSeriesExtractor;

import java.util.List;

import com.google.common.collect.Lists;
import com.sun.jersey.api.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility builder to prepare objects suitable for SOLR.
 */
class SolrAnnotatedDatasetBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SolrAnnotatedDatasetBuilder.class);

  private final NetworkEntityService<Organization> organizationService;
  private final NetworkEntityService<Installation> installationService;
  private final TimeSeriesExtractor timeSeriesExtractor = new TimeSeriesExtractor(1000, 2400, 1800, 2050);

  public SolrAnnotatedDatasetBuilder(NetworkEntityService<Organization> organizationService,
    NetworkEntityService<Installation> installationService) {
    this.organizationService = organizationService;
    this.installationService = installationService;
  }

  /**
   * Creates a SolrAnnotatedDataset from the given dataset, copying only the relevant fields for Solr from
   * the given dataset.
   * 
   * @param d The Dataset which will be copied into this object
   */
  public SolrAnnotatedDataset build(Dataset d) {
    SolrAnnotatedDataset sad = new SolrAnnotatedDataset();

    sad.setDescription(d.getDescription());
    sad.setKey(d.getKey());
    sad.setTitle(d.getTitle());
    sad.setType(d.getType());
    sad.setSubtype(d.getSubtype());
    // TODO: http://dev.gbif.org/issues/browse/REG-393
    sad.setCountryCoverage(d.getCountryCoverage());
    List<String> kw = Lists.newArrayList();
    for (Tag t : d.getTags()) {
      kw.add(t.getValue());
    }
    sad.setKeywords(kw);
    sad.setDecades(timeSeriesExtractor.extractDecades(d.getTemporalCoverages()));
    sad.setOwningOrganizationKey(d.getOwningOrganizationKey());

    // see http://dev.gbif.org/issues/browse/REG-405 which explains why we defend against NotFoundExceptions below

    Organization owner = null;
    try {
      owner = d.getOwningOrganizationKey() != null ? organizationService.get(d.getOwningOrganizationKey()) : null;
    } catch (NotFoundException e) {
      // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
      LOG.warn("Service reports organization[{}] cannot be found for dataset[{}]", d.getOwningOrganizationKey(),
        d.getKey());
    }

    Installation installation = null;
    try {
      installation = d.getInstallationKey() != null ? installationService.get(d.getInstallationKey()) : null;
    } catch (NotFoundException e) {
      // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
      LOG.warn("Service reports installation[{}] cannot be found for dataset[{}]", d.getInstallationKey(), d.getKey());
    }

    Organization host = null;
    try {
      host = installation != null && installation.getOrganizationKey() != null ? organizationService.get(installation
        .getOrganizationKey()) : null;
    } catch (NotFoundException e) {
      // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
      LOG.warn("Service reports organization[{}] cannot be found for installation[{}]",
        installation.getOrganizationKey(), installation.getKey());
    }

    if (owner != null) {
      sad.setOwningOrganizationTitle(owner.getTitle());
      if (owner.getCountry() != null) {
        sad.setPublishingCountry(owner.getCountry().getIso2LetterCode());
      }
    } else {
      sad.setPublishingCountry(Country.UNKNOWN);
    }
    if (host != null) {
      sad.setHostingOrganizationKey(String.valueOf(host.getKey()));
      sad.setHostingOrganizationTitle(host.getTitle());
    }

    return sad;
  }
}
