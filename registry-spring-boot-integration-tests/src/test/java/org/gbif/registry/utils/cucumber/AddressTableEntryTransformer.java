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
package org.gbif.registry.utils.cucumber;

import org.gbif.api.model.collections.Address;
import org.gbif.api.vocabulary.Country;

import java.util.Map;
import java.util.Optional;

import io.cucumber.datatable.TableEntryTransformer;

public class AddressTableEntryTransformer implements TableEntryTransformer<Address> {

  @Override
  public Address transform(Map<String, String> entry) {
    Address result = new Address();

    result.setCity(entry.get("city"));
    result.setProvince(entry.get("province"));
    Optional.ofNullable(entry.get("country")).map(Country::valueOf).ifPresent(result::setCountry);
    result.setAddress(entry.get("address"));
    result.setPostalCode(entry.get("postalCode"));

    return result;
  }
}
