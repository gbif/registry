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
import feign.RequestLine;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.registry.doi.DoiInteractionService;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.domain.doi.DoiType;

public interface DoiInteractionClient extends DoiInteractionService {

  // ---------------------------------------------------------------------------
  // GENERATE DOI
  // ---------------------------------------------------------------------------

  @RequestLine("POST /doi/gen/{type}")
  DOI generate(DoiType type);

  // ---------------------------------------------------------------------------
  // GET DOI DATA
  // ---------------------------------------------------------------------------

  @RequestLine("GET /doi/{prefix}/{suffix}")
  @Headers("Accept: application/json")
  DoiData get(String prefix, String suffix);

  // ---------------------------------------------------------------------------
  // REGISTER DOI
  // ---------------------------------------------------------------------------

  @RequestLine("POST /doi")
  @Headers("Content-Type: application/json")
  DOI register(DoiRegistration doiRegistration);

  // ---------------------------------------------------------------------------
  // UPDATE DOI
  // ---------------------------------------------------------------------------

  @RequestLine("PUT /doi")
  @Headers("Content-Type: application/json")
  DOI update(DoiRegistration doiRegistration);

  // ---------------------------------------------------------------------------
  // DELETE DOI
  // ---------------------------------------------------------------------------

  @RequestLine("DELETE /doi/{prefix}/{suffix}")
  void delete(String prefix, String suffix);
}
