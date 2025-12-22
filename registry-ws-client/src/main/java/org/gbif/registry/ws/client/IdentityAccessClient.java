/*
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
package org.gbif.registry.ws.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;

/**
 * Feign client for identity and access endpoints.
 *
 * Base path must be supplied when creating the client, e.g.:
 *   https://api.gbif.org/v1/
 */
@Headers("Accept: application/json")
public interface IdentityAccessClient extends IdentityAccessService {

  @RequestLine("GET admin/user/{username}")
  GbifUser get(@Param("username") String username);

  @Override
  default GbifUser authenticate(String username, String password) {
    throw new UnsupportedOperationException("Not implemented in WS client");
  }
}
