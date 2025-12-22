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

@Headers("Accept: application/json")
public interface OccurrenceDownloadClient extends BaseDownloadClient {
  // This interface inherits all methods from BaseDownloadClient
  // No additional methods are defined here, but Feign will use
  // the base URL path "occurrence/download" if configured in the Feign client
}
