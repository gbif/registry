package org.gbif.registry.guice;

import org.gbif.occurrence.query.TitleLookup;

import com.google.inject.AbstractModule;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides a mocked TitleLookup implementation.
 */
public class TitleLookupMockModule extends AbstractModule {

    @Override
    protected void configure() {
      TitleLookup tl = mock(TitleLookup.class);
      when(tl.getDatasetTitle(anyString())).thenReturn("PonTaurus");
      when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");

      bind(TitleLookup.class).toInstance(tl);
    }
}

