package org.gbif.registry.service.collections.lookup.matchers;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.service.collections.lookup.Matches;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.INSTITUTION_TO_COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.INST_COLL_MISMATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

@Component
public class CollectionMatcher extends BaseMatcher<Collection> {

  private final CollectionMapper collectionMapper;

  @Autowired
  public CollectionMatcher(CollectionMapper collectionMapper, DatasetService datasetService) {
    super(datasetService);
    this.collectionMapper = collectionMapper;
  }

  public Matches<Collection> matchCollections(LookupParams params, Set<UUID> institutionMatches) {
    Matches<Collection> matches = new Matches<>();

    matchWithMachineTags(params.getDatasetKey(), machineTagProcessor(params))
        .ifPresent(matches::setMachineTagMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findExactMatches(params, institutionMatches).ifPresent(matches::setExactMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findFuzzyMatches(params, matches, institutionMatches).ifPresent(matches::setFuzzyMatchesMap);

    return setAccepted(matches);
  }

  private Matches<Collection> setAccepted(Matches<Collection> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            extractMatches(matches.getMachineTagMatchesMap()),
            extractMatches(matches.getExactMatchesMap()),
            extractAndFilter(
                matches.getFuzzyMatchesMap(), m -> !m.getReasons().contains(INST_COLL_MISMATCH))));
    return matches;
  }

  private BiConsumer<MachineTag, Map<UUID, Match<Collection>>> machineTagProcessor(
      LookupParams params) {
    return (mt, matchesMap) -> {
      if (COLLECTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(mt, params.getCollectionCode(), matchesMap, COLLECTION_TAG);
      } else if (INSTITUTION_TO_COLLECTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(
            mt, params.getInstitutionCode(), matchesMap, INSTITUTION_TO_COLLECTION_TAG);
      }
    };
  }

  private Optional<Map<UUID, Match<Collection>>> findExactMatches(
      LookupParams params, Set<UUID> institutionMatches) {
    if (!params.containsCollectionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<Collection>> exactMatches = new HashMap<>();
    // find by code and identifier
    List<Collection> collectionsFoundByCodeAndId =
        findByCodeAndId(params.getCollectionCode(), params.getCollectionId());
    // if found we don't look for more matches since these are exact matches
    collectionsFoundByCodeAndId.forEach(
        c -> {
          Match<Collection> exactMatch = exact(c, CODE_MATCH, IDENTIFIER_MATCH);
          exactMatches.put(c.getKey(), exactMatch);
          checkCountryMatch(params, exactMatch);
          checkInstitutionCollectionMisMatch(exactMatch, institutionMatches);
        });

    return exactMatches.isEmpty() ? Optional.empty() : Optional.of(exactMatches);
  }

  private Optional<Map<UUID, Match<Collection>>> findFuzzyMatches(
      LookupParams params, Matches<Collection> matches, Set<UUID> institutionMatches) {
    if (!params.containsCollectionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<Collection>> fuzzyMatches = new HashMap<>();
    BiConsumer<List<Collection>, Match.Reason> addFuzzyCollMatch =
        (l, r) ->
            l.stream()
                .filter(c -> !matches.getExactMatchesMap().containsKey(c.getKey()))
                .forEach(
                    c -> {
                      Match<Collection> fuzzyMatch =
                          fuzzyMatches.computeIfAbsent(c.getKey(), k -> fuzzy(c)).addReason(r);
                      checkCountryMatch(params, fuzzyMatch);
                      checkInstitutionCollectionMisMatch(fuzzyMatch, institutionMatches);
                    });

    // find matches by code
    addFuzzyCollMatch.accept(findByCode(params.getCollectionCode()), CODE_MATCH);

    // find matches by identifier
    addFuzzyCollMatch.accept(findByIdentifier(params.getCollectionId()), IDENTIFIER_MATCH);

    // find matches by alternative code
    addFuzzyCollMatch.accept(
        findByAlternativeCode(params.getCollectionCode()), ALTERNATIVE_CODE_MATCH);

    // find matches using the code as name
    addFuzzyCollMatch.accept(findByName(params.getCollectionCode()), NAME_MATCH);

    // find matches using the id as name
    addFuzzyCollMatch.accept(findByName(params.getCollectionId()), NAME_MATCH);

    return fuzzyMatches.isEmpty() ? Optional.empty() : Optional.of(fuzzyMatches);
  }

  private void checkInstitutionCollectionMisMatch(
      Match<Collection> match, Set<UUID> institutionMatches) {
    if (!isMatchWithInstitutions(match.getEntityMatched(), institutionMatches)) {
      match.addReason(INST_COLL_MISMATCH);
    }
  }

  private boolean isMatchWithInstitutions(Collection c, Set<UUID> institutionMatches) {
    return institutionMatches.isEmpty()
        || c.getInstitutionKey() == null
        || institutionMatches.contains(c.getInstitutionKey());
  }

  @Override
  LookupMapper<Collection> getLookupMapper() {
    return collectionMapper;
  }

  @Override
  BaseMapper<Collection> getBaseMapper() {
    return collectionMapper;
  }

  @Override
  Predicate<MachineTag> isMachineTagSupported() {
    return mt ->
        mt.getNamespace().equals(PROCESSING_NAMESPACE)
            && Arrays.asList(COLLECTION_TAG_NAME, INSTITUTION_TO_COLLECTION_TAG_NAME)
                .contains(mt.getName());
  }
}
