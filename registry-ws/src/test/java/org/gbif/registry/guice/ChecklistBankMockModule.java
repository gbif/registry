package org.gbif.registry.guice;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.AbstractModule;

/**
 * Provides a mocked NameUsageService.get(id) implementation.
 */
public class ChecklistBankMockModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(NameUsageService.class).to(NameUsageServiceMock.class);
    }

  private static class NameUsageServiceMock implements NameUsageService {

    @Nullable
    @Override
    public NameUsage get(int taxonKey, Locale locale) {
      NameUsage u = new NameUsage();
      u.setKey(taxonKey);
      u.setNubKey(taxonKey);
      u.setScientificName("Abies alba Mill.");
      u.setCanonicalName("Abies alba");
      u.setFamily("Pinaceae");
      u.setKingdom(Kingdom.PLANTAE.scientificName());
      u.setKingdomKey(Kingdom.PLANTAE.nubUsageID());
      u.setRank(Rank.SPECIES);
      u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
      u.setDatasetKey(Constants.NUB_DATASET_KEY);
      return u;
    }

    @Nullable
    @Override
    public ParsedName getParsedName(int taxonKey) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Nullable
    @Override
    public NameUsageMetrics getMetrics(int taxonKey) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Nullable
    @Override
    public VerbatimNameUsage getVerbatim(int taxonKey) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PagingResponse<NameUsage> list(Locale locale, @Nullable UUID datasetKey, @Nullable String sourceId,
      @Nullable Pageable page) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PagingResponse<NameUsage> listByCanonicalName(Locale locale, String canonicalName, @Nullable Pageable page,
      @Nullable UUID... datasetKey) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PagingResponse<NameUsage> listChildren(int parentKey, Locale locale, @Nullable Pageable page) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<NameUsage> listParents(int taxonKey, Locale locale) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<NameUsage> listRelated(int taxonKey, Locale locale, @Nullable UUID... datasetKey) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PagingResponse<NameUsage> listRoot(UUID datasetKey, Locale locale, @Nullable Pageable page) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PagingResponse<NameUsage> listSynonyms(int taxonKey, Locale locale, @Nullable Pageable page) {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }
}

