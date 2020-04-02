/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.guice;

import org.gbif.occurrence.query.TitleLookupService;

import com.google.inject.AbstractModule;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Provides a mocked TitleLookup implementation. */
public class TitleLookupMockModule extends AbstractModule {

  @Override
  protected void configure() {
    TitleLookupService tl = mock(TitleLookupService.class);
    when(tl.getDatasetTitle(anyString())).thenReturn("PonTaurus");
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");

    bind(TitleLookupService.class).toInstance(tl);
  }
}
