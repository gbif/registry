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

import org.gbif.api.model.common.GbifUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.cucumber.datatable.TableEntryTransformer;

public class GbifUserTableEntryTransformer implements TableEntryTransformer<GbifUser> {

  @Override
  public GbifUser transform(Map<String, String> entry) {
    GbifUser result = new GbifUser();
    Optional.ofNullable(entry.get("key")).map(Integer::parseInt).ifPresent(result::setKey);
    result.setUserName(entry.get("userName"));
    result.setFirstName(entry.get("firstName"));
    result.setLastName(entry.get("lastName"));
    result.setEmail(entry.get("email"));
    Optional.ofNullable(entry.get("roles"))
        .map(roles -> roles.split(","))
        .map(Arrays::asList)
        .map((Function<List<String>, HashSet>) HashSet::new)
        .ifPresent(result::setRoles);

    Optional.ofNullable(entry.get("settings"))
        .map(settings -> settings.split(","))
        .map(Arrays::stream)
        .map(
            keyValuePairStream ->
                keyValuePairStream
                    .map(keyValuePair -> keyValuePair.split("=>"))
                    .collect(
                        Collectors.toMap(
                            keyAndValue -> keyAndValue[0].trim(),
                            keyAndValue -> keyAndValue[1].trim())))
        .ifPresent(result::setSettings);

    Optional.ofNullable(entry.get("systemSettings"))
        .map(settings -> settings.split(","))
        .map(Arrays::stream)
        .map(
            keyValuePairStream ->
                keyValuePairStream
                    .map(keyValuePair -> keyValuePair.split("=>"))
                    .collect(
                        Collectors.toMap(
                            keyAndValue -> keyAndValue[0].trim(),
                            keyAndValue -> keyAndValue[1].trim())))
        .ifPresent(result::setSystemSettings);

    return result;
  }
}
