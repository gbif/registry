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
package org.gbif.registry.service.collections.lookup;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.lookup.Match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Wraps the response from the GrSciColl matchers. */
public class Matches<T extends CollectionEntity> {
  private Map<UUID, Match<T>> machineTagMatchesMap = new HashMap<>();
  private Map<UUID, Match<T>> exactMatchesMap = new HashMap<>();
  private Map<UUID, Match<T>> fuzzyMatchesMap = new HashMap<>();
  private Match<T> acceptedMatch;

  public Set<Match<T>> getAllMatches() {
    Set<Match<T>> all = new HashSet<>();
    if (machineTagMatchesMap != null) {
      all.addAll(machineTagMatchesMap.values());
    }
    if (exactMatchesMap != null) {
      all.addAll(exactMatchesMap.values());
    }
    if (fuzzyMatchesMap != null) {
      all.addAll(fuzzyMatchesMap.values());
    }
    return all;
  }

  public boolean isEmpty() {
    return machineTagMatchesMap.isEmpty() && exactMatchesMap.isEmpty() && fuzzyMatchesMap.isEmpty();
  }

  public Map<UUID, Match<T>> getMachineTagMatchesMap() {
    return machineTagMatchesMap;
  }

  public void setMachineTagMatchesMap(Map<UUID, Match<T>> machineTagMatchesMap) {
    this.machineTagMatchesMap = machineTagMatchesMap;
  }

  public Map<UUID, Match<T>> getExactMatchesMap() {
    return exactMatchesMap;
  }

  public void setExactMatchesMap(Map<UUID, Match<T>> exactMatchesMap) {
    this.exactMatchesMap = exactMatchesMap;
  }

  public Map<UUID, Match<T>> getFuzzyMatchesMap() {
    return fuzzyMatchesMap;
  }

  public void setFuzzyMatchesMap(Map<UUID, Match<T>> fuzzyMatchesMap) {
    this.fuzzyMatchesMap = fuzzyMatchesMap;
  }

  public Match<T> getAcceptedMatch() {
    return acceptedMatch;
  }

  public void setAcceptedMatch(Match<T> acceptedMatch) {
    this.acceptedMatch = acceptedMatch;
  }
}
