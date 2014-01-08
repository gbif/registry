/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.provider;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.ws.server.provider.FacetedSearchRequestProvider;

import javax.ws.rs.ext.Provider;

import com.google.inject.Singleton;

@Provider
@Singleton
public class DatasetSearchRequestProvider extends
  FacetedSearchRequestProvider<DatasetSearchRequest, DatasetSearchParameter> {

  public DatasetSearchRequestProvider() {
    super(DatasetSearchRequest.class, DatasetSearchParameter.class);
  }

}
