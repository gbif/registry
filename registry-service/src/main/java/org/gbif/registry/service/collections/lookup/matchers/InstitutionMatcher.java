package org.gbif.registry.service.collections.lookup.matchers;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.service.collections.lookup.Matches;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COLLECTION_TO_INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.PROBABLY_ON_LOAN;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

@Component
public class InstitutionMatcher extends BaseMatcher<Institution> {

  private final InstitutionMapper institutionMapper;

  @Autowired
  public InstitutionMatcher(InstitutionMapper institutionMapper, DatasetService datasetService) {
    super(datasetService);
    this.institutionMapper = institutionMapper;
  }

  public Matches<Institution> matchInstitutions(LookupParams params) {
    Matches<Institution> matches = new Matches<>();

    matchWithMachineTags(params.getDatasetKey(), machineTagProcessor(params))
        .ifPresent(matches::setMachineTagMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findExactMatches(params).ifPresent(matches::setExactMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findFuzzyMatches(params, matches).ifPresent(matches::setFuzzyMatchesMap);

    return setAccepted(matches);
  }

  private Matches<Institution> setAccepted(Matches<Institution> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            extractMatches(matches.getMachineTagMatchesMap()),
            extractAndFilter(
                matches.getExactMatchesMap(), m -> !m.getReasons().contains(PROBABLY_ON_LOAN)),
            extractAndFilter(
                matches.getFuzzyMatchesMap(), m -> !m.getReasons().contains(PROBABLY_ON_LOAN))));
    return matches;
  }

  private BiConsumer<MachineTag, Map<UUID, Match<Institution>>> machineTagProcessor(
      LookupParams params) {
    return (mt, matchesMap) -> {
      if (INSTITUTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(mt, params.getInstitutionCode(), matchesMap, INSTITUTION_TAG);
      } else if (COLLECTION_TO_INSTITUTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(
            mt, params.getCollectionCode(), matchesMap, COLLECTION_TO_INSTITUTION_TAG);
      }
    };
  }

  private Optional<Map<UUID, Match<Institution>>> findExactMatches(LookupParams params) {
    if (!params.containsInstitutionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<Institution>> exactMatches = new HashMap<>();
    // find by code and identifier.
    List<Institution> institutionsFoundByCodeAndId =
        findByCodeAndId(params.getInstitutionCode(), params.getInstitutionId());
    // if found we don't look for more matches since these are exact matches
    institutionsFoundByCodeAndId.forEach(
        i -> {
          Match<Institution> exactMatch = exact(i, CODE_MATCH, IDENTIFIER_MATCH);
          exactMatches.put(i.getKey(), exactMatch);
          checkCountryMatch(params, exactMatch);
          checkOwnerInstitutionMismatch(params, exactMatch);
        });

    return exactMatches.isEmpty() ? Optional.empty() : Optional.of(exactMatches);
  }

  private Optional<Map<UUID, Match<Institution>>> findFuzzyMatches(
      LookupParams params, Matches<Institution> allMatches) {
    if (!params.containsInstitutionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<Institution>> fuzzyMatches = new HashMap<>();
    BiConsumer<List<Institution>, Match.Reason> addFuzzyInstMatch =
        (l, r) ->
            l.stream()
                .filter(i -> !allMatches.getExactMatchesMap().containsKey(i.getKey()))
                .forEach(
                    i -> {
                      Match<Institution> fuzzyMatch =
                          fuzzyMatches.computeIfAbsent(i.getKey(), k -> fuzzy(i)).addReason(r);
                      checkCountryMatch(params, fuzzyMatch);
                      checkOwnerInstitutionMismatch(params, fuzzyMatch);
                    });

    // find matches by code
    addFuzzyInstMatch.accept(findByCode(params.getInstitutionCode()), CODE_MATCH);

    // find matches by identifier
    addFuzzyInstMatch.accept(findByIdentifier(params.getInstitutionId()), IDENTIFIER_MATCH);

    // find matches by alternative code
    addFuzzyInstMatch.accept(
        findByAlternativeCode(params.getInstitutionCode()), ALTERNATIVE_CODE_MATCH);

    // find matches by using the code as name
    addFuzzyInstMatch.accept(findByName(params.getInstitutionCode()), NAME_MATCH);

    // find matches by using the id as name
    addFuzzyInstMatch.accept(findByName(params.getInstitutionId()), NAME_MATCH);

    return fuzzyMatches.isEmpty() ? Optional.empty() : Optional.of(fuzzyMatches);
  }

  private void checkOwnerInstitutionMismatch(LookupParams params, Match<Institution> match) {
    if (!Strings.isNullOrEmpty(params.getOwnerInstitutionCode())
        && !match.getEntityMatched().getCode().equals(params.getOwnerInstitutionCode())
        && !match.getEntityMatched().getName().equals(params.getOwnerInstitutionCode())) {
      match.getReasons().add(PROBABLY_ON_LOAN);
    }
  }

  @Override
  LookupMapper<Institution> getLookupMapper() {
    return institutionMapper;
  }

  @Override
  BaseMapper<Institution> getBaseMapper() {
    return institutionMapper;
  }

  @Override
  Predicate<MachineTag> isMachineTagSupported() {
    return mt ->
        mt.getNamespace().equals(PROCESSING_NAMESPACE)
            && Arrays.asList(INSTITUTION_TAG_NAME, COLLECTION_TO_INSTITUTION_TAG_NAME)
                .contains(mt.getName());
  }
}
