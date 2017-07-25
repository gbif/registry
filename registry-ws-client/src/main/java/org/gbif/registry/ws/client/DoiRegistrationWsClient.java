/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.ws.client;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsClient;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Client-side implementation to the DoiRegistrationService.
 */
public class DoiRegistrationWsClient extends BaseWsClient implements DoiRegistrationService {

  @Inject
  public DoiRegistrationWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(resource.path("doi"));
    if (authFilter != null) {
      this.resource.addFilter(authFilter);
    }
  }

  @Override
  public DOI generate(DoiType doiType) {
    return getResource("gen", doiType.name()).type(MediaType.APPLICATION_JSON).post(DOI.class);
  }

  @Override
  public DoiData get(String prefix, String suffix) {
    return getResource(prefix, suffix).type(MediaType.APPLICATION_JSON).get(DoiData.class);
  }

  @Override
  public void delete(String prefix, String suffix) {
    getResource(prefix, suffix).type(MediaType.APPLICATION_JSON).delete();
  }

  @Override
  public DOI register(DoiRegistration doiRegistration) {
    return resource.type(MediaType.APPLICATION_JSON).post(DOI.class, toBytes(doiRegistration));
  }

  @Override
  public DOI update(DoiRegistration doiRegistration) {
    return resource.type(MediaType.APPLICATION_JSON).put(DOI.class, toBytes(doiRegistration));
  }

}
