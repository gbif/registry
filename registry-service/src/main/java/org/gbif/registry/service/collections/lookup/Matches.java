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
package org.gbif.registry.service.collections.lookup;

import org.gbif.api.model.collections.lookup.EntityMatched;
import org.gbif.api.model.collections.lookup.Match;

import java.util.HashSet;
import java.util.Set;

/** Wraps the response from the GrSciColl matchers. */
public class Matches<T extends EntityMatched> {
  private Set<Match<T>> explicitMatches = new HashSet<>();
  private Set<Match<T>> exactMatches = new HashSet<>();
  private Set<Match<T>> fuzzyMatches = new HashSet<>();
  private Match<T> acceptedMatch;

  public Set<Match<T>> getAllMatches() {
    Set<Match<T>> all = new HashSet<>();
    if (explicitMatches != null) {
      all.addAll(explicitMatches);
    }
    if (exactMatches != null) {
      all.addAll(exactMatches);
    }
    if (fuzzyMatches != null) {
      all.addAll(fuzzyMatches);
    }
    return all;
  }

  public boolean isEmpty() {
    return explicitMatches.isEmpty() && exactMatches.isEmpty() && fuzzyMatches.isEmpty();
  }

  public Set<Match<T>> getExplicitMatches() {
    return explicitMatches;
  }

  public void setExplicitMatches(Set<Match<T>> explicitMatches) {
    this.explicitMatches = explicitMatches;
  }

  public Set<Match<T>> getExactMatches() {
    return exactMatches;
  }

  public void setExactMatches(Set<Match<T>> exactMatches) {
    this.exactMatches = exactMatches;
  }

  public Set<Match<T>> getFuzzyMatches() {
    return fuzzyMatches;
  }

  public void setFuzzyMatches(Set<Match<T>> fuzzyMatches) {
    this.fuzzyMatches = fuzzyMatches;
  }

  public Match<T> getAcceptedMatch() {
    return acceptedMatch;
  }

  public void setAcceptedMatch(Match<T> acceptedMatch) {
    this.acceptedMatch = acceptedMatch;
  }
}
