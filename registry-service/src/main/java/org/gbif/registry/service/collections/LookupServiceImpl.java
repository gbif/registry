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
package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.MatchRemark;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.INST_COLL_MISMATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.PROBABLY_ON_LOAN;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

@Service
public class LookupServiceImpl implements LookupService {

  private final InstitutionMapper institutionMapper;
  private final CollectionMapper collectionMapper;

  private static final Comparator<Match.MatchType> MATCH_TYPE_COMPARATOR =
      (t1, t2) -> {
        if (t1 == null) {
          return t2 == null ? 0 : 1;
        } else if (t2 == null) {
          return -1;
        }

        if (t1 == t2) {
          return 0;
        }
        if (t1 == Match.MatchType.EXACT) {
          return -1;
        }
        if (t2 == Match.MatchType.EXACT) {
          return 1;
        }
        return t1.compareTo(t2);
      };

  @Autowired
  public LookupServiceImpl(InstitutionMapper institutionMapper, CollectionMapper collectionMapper) {
    this.institutionMapper = institutionMapper;
    this.collectionMapper = collectionMapper;
  }

  @Override
  public LookupResult lookup(LookupParams params) {
    if (params.getDatasetKey() != null) {
      // TODO: check tags
    }

    LookupResult result = new LookupResult();

    Map<UUID, Match<Institution>> institutionMatchesMap = matchInstitutions(params);
    Map<UUID, Match<Collection>> collectionMatchesMap =
        matchCollections(params, institutionMatchesMap);

    List<Match<Institution>> institutionMatches = new ArrayList<>(institutionMatchesMap.values());
    List<Match<Collection>> collectionMatches = new ArrayList<>(collectionMatchesMap.values());

    institutionMatches.sort(Comparator.comparing(Match::getType, MATCH_TYPE_COMPARATOR));
    collectionMatches.sort(Comparator.comparing(Match::getType, MATCH_TYPE_COMPARATOR));

    result.setInstitutionMatches(institutionMatches);
    result.setCollectionMatches(collectionMatches);

    return result;
  }

  private Map<UUID, Match<Institution>> matchInstitutions(LookupParams params) {
    // map to keep track of the matches
    Map<UUID, Match<Institution>> institutionMatches = new HashMap<>();
    if (params.containsInstitutionParams()) {
      // find by code and identifier.
      List<Institution> institutionsFoundByCodeAndId =
          findByCodeAndId(
              params.getInstitutionCode(), params.getInstitutionId(), institutionMapper);
      if (!institutionsFoundByCodeAndId.isEmpty()) {
        // if found we don't look for more matches since these are exact matches
        institutionsFoundByCodeAndId.forEach(
            i -> institutionMatches.put(i.getKey(), exact(i, CODE_MATCH, IDENTIFIER_MATCH)));
      } else {

        BiConsumer<List<Institution>, MatchRemark> addFuzzyInstMatch =
            (l, r) ->
                l.forEach(
                    i ->
                        institutionMatches.computeIfAbsent(i.getKey(), k -> fuzzy(i)).addRemark(r));

        // find matches by code
        addFuzzyInstMatch.accept(
            findByCode(params.getInstitutionCode(), institutionMapper), CODE_MATCH);

        // find matches by identifier
        addFuzzyInstMatch.accept(
            findByIdentifier(params.getInstitutionId(), institutionMapper), IDENTIFIER_MATCH);

        // find matches by alternative code
        addFuzzyInstMatch.accept(
            findByAlternativeCode(params.getInstitutionCode(), institutionMapper),
            ALTERNATIVE_CODE_MATCH);

        // if no matches we try using the code as name
        if (institutionMatches.isEmpty()) {
          addFuzzyInstMatch.accept(
              findByName(params.getInstitutionCode(), institutionMapper), NAME_MATCH);
        }
      }

      // if the owner institution is different from the matches we add a remark
      if (!Strings.isNullOrEmpty(params.getOwnerInstitutionCode())) {
        institutionMatches
            .values()
            .forEach(
                m -> {
                  if (!m.getEntityMatched().getCode().equals(params.getOwnerInstitutionCode())
                      && !m.getEntityMatched().getName().equals(params.getOwnerInstitutionCode())) {
                    m.getRemarks().add(PROBABLY_ON_LOAN);
                  }
                });
      }
    }

    return institutionMatches;
  }

  public Map<UUID, Match<Collection>> matchCollections(
      LookupParams params, Map<UUID, Match<Institution>> institutionMatches) {
    Map<UUID, Match<Collection>> collectionMatches = new HashMap<>();

    Predicate<Collection> matchesInstitutions =
        c ->
            institutionMatches.isEmpty()
                || c.getInstitutionKey() == null
                || institutionMatches.containsKey(c.getInstitutionKey());

    if (params.containsCollectionParams()) {
      // find by code and identifier.
      List<Collection> collectionsFoundByCodeAndId =
          findByCodeAndId(params.getCollectionCode(), params.getCollectionId(), collectionMapper);
      if (!collectionsFoundByCodeAndId.isEmpty()) {
        // if found we don't look for more matches since these are exact matches
        collectionsFoundByCodeAndId.forEach(
            c -> {
              Match<Collection> match = exact(c, CODE_MATCH, IDENTIFIER_MATCH);
              if (!matchesInstitutions.test(c)) {
                match.addRemark(INST_COLL_MISMATCH);
              }
              collectionMatches.put(c.getKey(), match);
            });
      } else {

        BiConsumer<List<Collection>, MatchRemark> addFuzzyCollMatch =
            (l, r) ->
                l.forEach(
                    c -> {
                      List<MatchRemark> remarks = new ArrayList<>(Collections.singletonList(r));
                      if (matchesInstitutions.test(c)) {
                        collectionMatches
                            .computeIfAbsent(c.getKey(), k -> fuzzy(c))
                            .getRemarks()
                            .addAll(remarks);
                      }
                    });

        // find matches by code
        addFuzzyCollMatch.accept(
            findByCode(params.getCollectionCode(), collectionMapper), CODE_MATCH);

        // find matches by identifier
        addFuzzyCollMatch.accept(
            findByIdentifier(params.getCollectionId(), collectionMapper), IDENTIFIER_MATCH);

        // find matches by alternative code
        addFuzzyCollMatch.accept(
            findByAlternativeCode(params.getCollectionCode(), collectionMapper),
            ALTERNATIVE_CODE_MATCH);

        // if no matches we try using the code as name
        if (collectionMatches.isEmpty()) {
          addFuzzyCollMatch.accept(
              findByName(params.getCollectionCode(), collectionMapper), NAME_MATCH);
        }
      }
    }

    return collectionMatches;
  }

  private <T extends CollectionEntity> List<T> findByCodeAndId(
      String code, String id, LookupMapper<T> mapper) {
    if (!Strings.isNullOrEmpty(code) && !Strings.isNullOrEmpty(id)) {
      return mapper.lookup(code, id, null, null);
    }
    return Collections.emptyList();
  }

  private <T extends CollectionEntity> List<T> findByAlternativeCode(
      String alternativeCode, LookupMapper<T> mapper) {
    if (!Strings.isNullOrEmpty(alternativeCode)) {
      return mapper.lookup(null, null, null, alternativeCode);
    }
    return Collections.emptyList();
  }

  private <T extends CollectionEntity> List<T> findByCode(String code, LookupMapper<T> mapper) {
    if (!Strings.isNullOrEmpty(code)) {
      return mapper.lookup(code, null, null, null);
    }
    return Collections.emptyList();
  }

  private <T extends CollectionEntity> List<T> findByName(String code, LookupMapper<T> mapper) {
    if (!Strings.isNullOrEmpty(code)) {
      return mapper.lookup(null, null, code, null);
    }
    return Collections.emptyList();
  }

  private <T extends CollectionEntity> List<T> findByIdentifier(String id, LookupMapper<T> mapper) {
    if (!Strings.isNullOrEmpty(id)) {
      return mapper.lookup(null, id, null, null);
    }
    return Collections.emptyList();
  }
}
