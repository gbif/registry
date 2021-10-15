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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.domain.doi.DoiType;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/** Specifies the contract of a service that manages DOI registrations. */
public interface DoiInteractionService {

  /** Generates a new DOI based on the DoiType. */
  DOI generate(@NotNull DoiType doiType);

  /** Retrieves the DOI information. */
  DoiData get(@NotNull String prefix, @NotNull String suffix);

  /**
   * Register a new DOI, if the registration object doesn't contain a DOI a new DOI is generated.
   */
  DOI register(@NotNull @Valid DoiRegistration doiRegistration);

  /** Update a DOI registration data. */
  DOI update(@NotNull @Valid DoiRegistration doiRegistration);

  /** Deletes a DOI. */
  void delete(@NotNull String prefix, @NotNull String suffix);
}
