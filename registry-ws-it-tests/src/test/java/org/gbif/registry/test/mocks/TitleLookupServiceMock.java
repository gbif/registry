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
package org.gbif.registry.test.mocks;
import org.gbif.occurrence.query.TitleLookupService;


/** Mock service, returns always the keys as titles. */
public class TitleLookupServiceMock implements TitleLookupService {

  @Override
  public String getDatasetTitle(String datasetKey) {
     return datasetKey;
  }

  @Override
  public String getSpeciesName(String speciesKey) {
    return speciesKey;
  }
}
