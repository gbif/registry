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
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.MATCH_TYPE_COMPARATOR;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.COLLECTION_TO_INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.INSTITUTION_TO_COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.INST_COLL_MISMATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.MatchRemark.PROBABLY_ON_LOAN;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

@Service
public class LookupServiceImpl implements LookupService {

  public static final String PROCESSING_NAMESPACE = "processing.gbif.org";
  public static final String INSTITUTION_TAG_NAME = "institutionCode";
  public static final String COLLECTION_TAG_NAME = "collectionCode";
  public static final String COLLECTION_TO_INSTITUTION_TAG_NAME = "collectionToInstitutionCode";
  public static final String INSTITUTION_TO_COLLECTION_TAG_NAME = "institutionToCollectionCode";

  private static final List<String> GRSCICOLL_TAG_NAMES =
      Arrays.asList(
          INSTITUTION_TAG_NAME,
          COLLECTION_TAG_NAME,
          COLLECTION_TO_INSTITUTION_TAG_NAME,
          INSTITUTION_TO_COLLECTION_TAG_NAME);

  private static final Predicate<MachineTag> IS_GRSCICOLL_TAG =
      mt ->
          mt.getNamespace().equals(PROCESSING_NAMESPACE)
              && GRSCICOLL_TAG_NAMES.contains(mt.getName());

  private final InstitutionMapper institutionMapper;
  private final CollectionMapper collectionMapper;
  private final DatasetService datasetService;

  @Autowired
  public LookupServiceImpl(
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper,
      DatasetService datasetService) {
    this.institutionMapper = institutionMapper;
    this.collectionMapper = collectionMapper;
    this.datasetService = datasetService;
  }

  @Override
  public LookupResult lookup(LookupParams params) {
    if (params.getDatasetKey() != null) {
      // we check the machine tags and if we find matches we return the result already
      Optional<LookupResult> result = matchWithMachineTags(params);
      if (result.isPresent()) {
        return result.get();
      }
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

  private Optional<LookupResult> matchWithMachineTags(LookupParams params) {
    Dataset dataset = datasetService.get(params.getDatasetKey());
    if (dataset == null) {
      return Optional.empty();
    }

    // map to keep track of the matches
    Map<UUID, Match<Institution>> institutionMatches = new HashMap<>();
    Map<UUID, Match<Collection>> collectionMatches = new HashMap<>();
    dataset.getMachineTags().stream()
        .filter(IS_GRSCICOLL_TAG)
        .forEach(
            mt -> {
              if (INSTITUTION_TAG_NAME.equals(mt.getName())) {
                addMachineTagMatch(
                    mt,
                    params.getInstitutionCode(),
                    institutionMapper::get,
                    institutionMatches,
                    INSTITUTION_TAG);
              } else if (COLLECTION_TAG_NAME.equals(mt.getName())) {
                addMachineTagMatch(
                    mt,
                    params.getCollectionCode(),
                    collectionMapper::get,
                    collectionMatches,
                    COLLECTION_TAG);
              } else if (COLLECTION_TO_INSTITUTION_TAG_NAME.equals(mt.getName())) {
                addMachineTagMatch(
                    mt,
                    params.getCollectionCode(),
                    institutionMapper::get,
                    institutionMatches,
                    COLLECTION_TO_INSTITUTION_TAG);
              } else if (INSTITUTION_TO_COLLECTION_TAG_NAME.equals(mt.getName())) {
                addMachineTagMatch(
                    mt,
                    params.getInstitutionCode(),
                    collectionMapper::get,
                    collectionMatches,
                    INSTITUTION_TO_COLLECTION_TAG);
              }
            });

    if (institutionMatches.isEmpty() && collectionMatches.isEmpty()) {
      return Optional.empty();
    }

    LookupResult result = new LookupResult();
    institutionMatches.values().forEach(m -> result.getInstitutionMatches().add(m));
    collectionMatches.values().forEach(m -> result.getCollectionMatches().add(m));

    return Optional.of(result);
  }

  private <T extends CollectionEntity> void addMachineTagMatch(
      MachineTag mt,
      String param,
      Function<UUID, T> entityRetriever,
      Map<UUID, Match<T>> matchesMap,
      MatchRemark remark) {
    if (Strings.isNullOrEmpty(param)) {
      return;
    }

    String[] val = mt.getValue().split(":");
    if (val.length > 1) {
      UUID mtKey = UUID.fromString(val[0]);
      String mtCode = val[1];

      if (!Strings.isNullOrEmpty(param) && mtCode.equals(param)) {
        T entity = entityRetriever.apply(mtKey);
        if (entity != null) {
          matchesMap.computeIfAbsent(mtKey, k -> Match.machineTag(entity)).addRemark(remark);
        }
      }
    }
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
