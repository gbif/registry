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
package org.gbif.registry.ws.client.collections;
import feign.Param;
import feign.RequestLine;
import feign.Headers;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.vocabulary.Country;

import java.util.UUID;

public interface LookupClient {

  @RequestLine("GET /grscicoll/lookup?datasetKey={datasetKey}&institutionCode={institutionCode}&institutionId={institutionId}&ownerInstitutionCode={ownerInstitutionCode}&collectionCode={collectionCode}&collectionId={collectionId}&country={country}&verbose={verbose}")
  @Headers("Accept: application/json")
  LookupResult lookup(
    @Param("datasetKey") UUID datasetKey,
    @Param("institutionCode") @Trim String institutionCode,
    @Param("institutionId") @Trim String institutionId,
    @Param("ownerInstitutionCode") @Trim String ownerInstitutionCode,
    @Param("collectionCode") @Trim String collectionCode,
    @Param("collectionId") @Trim String collectionId,
    @Param("country") Country country,
    @Param("verbose") Boolean verbose
  );
}
