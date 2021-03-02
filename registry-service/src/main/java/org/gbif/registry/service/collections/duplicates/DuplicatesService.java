package org.gbif.registry.service.collections.duplicates;

import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.registry.persistence.mapper.collections.DuplicatesMapper;
import org.gbif.registry.persistence.mapper.collections.dto.DuplicateDto;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DuplicatesService {

  private static final Logger LOG = LoggerFactory.getLogger(DuplicatesService.class);

  private final DuplicatesMapper duplicatesMapper;

  @Autowired
  public DuplicatesService(DuplicatesMapper duplicatesMapper) {
    this.duplicatesMapper = duplicatesMapper;
  }

  public DuplicatesResult findPossibleDuplicateInstitutions(DuplicatesSearchParams params) {
    return processDBResults(duplicatesMapper::getInstitutionDuplicates, params);
  }

  public DuplicatesResult findPossibleDuplicateCollections(DuplicatesSearchParams params) {
    return processDBResults(duplicatesMapper::getCollectionDuplicates, params);
  }

  private DuplicatesResult processDBResults(
      Function<DuplicatesSearchParams, List<DuplicateDto>> dtosFn, DuplicatesSearchParams params) {
    List<DuplicateDto> dtos = dtosFn.apply(params);

    if (dtos.isEmpty()) {
      return new DuplicatesResult();
    }

    boolean transitive = assumeTransitiveClusters(params);

    // group by key1
    Map<UUID, List<DuplicateDto>> duplicatesMap =
        dtos.stream().collect(Collectors.groupingBy(DuplicateDto::getKey1));

    // list to store the groups of duplicates
    Map<Set<UUID>, Set<Duplicate>> duplicates = new HashMap<>();

    // we sort them so the bigger groups are created first and it's easier to find subsets of these
    // groups that should be discarded
    duplicatesMap.values().stream()
        // we sort them so the bigger groups are created first and it's easier to find subsets of
        // these groups that should be discarded. Specially useful for non-transitive clusters
        .sorted(Comparator.comparing(List::size, Comparator.reverseOrder()))
        .forEach(
            groupValues -> {
              // create a new group of duplicates
              Set<Duplicate> duplicatesGroup = new HashSet<>();
              // store the keys of the group
              Set<UUID> groupKeys = new HashSet<>();

              // iterate over the dtos. Each dto will result in 2 duplicates
              groupValues.forEach(
                  v -> {
                    Duplicate duplicate1 = new Duplicate();
                    duplicate1.setKey(v.getKey1());
                    duplicate1.setCode(v.getCode1());
                    duplicate1.setName(v.getName1());
                    duplicate1.setPhysicalCity(v.getPhysicalCity1());
                    duplicate1.setPhysicalCountry(v.getPhysicalCountry1());
                    duplicate1.setMailingCity(v.getMailingCity1());
                    duplicate1.setMailingCountry(v.getMailingCountry1());
                    duplicate1.setInstitutionKey(v.getInstitutionKey1());
                    duplicatesGroup.add(duplicate1);
                    groupKeys.add(v.getKey1());

                    Duplicate duplicate2 = new Duplicate();
                    duplicate2.setKey(v.getKey2());
                    duplicate2.setCode(v.getCode2());
                    duplicate2.setName(v.getName2());
                    duplicate2.setPhysicalCity(v.getPhysicalCity2());
                    duplicate2.setPhysicalCountry(v.getPhysicalCountry2());
                    duplicate2.setMailingCity(v.getMailingCity2());
                    duplicate2.setMailingCountry(v.getMailingCountry2());
                    duplicate2.setInstitutionKey(v.getInstitutionKey2());
                    duplicatesGroup.add(duplicate2);
                    groupKeys.add(v.getKey2());
                  });

              List<Set<UUID>> groupsFound;
              if (transitive) {
                // for transitive clusters we check if there is a group that contains at least 1 of
                // the keys of this new group
                groupsFound =
                    duplicates.keySet().stream()
                        .filter(g -> !Collections.disjoint(g, groupKeys))
                        .collect(Collectors.toList());
              } else {
                // for non-transitive clusters we check if there is a group that already contains
                // all the keys of this new group so we merge groups
                groupsFound =
                    duplicates.keySet().stream()
                        .filter(g -> g.containsAll(groupKeys))
                        .collect(Collectors.toList());
              }

              if (groupsFound.isEmpty()) {
                // the group doesn't exist so we add it
                duplicates.put(groupKeys, duplicatesGroup);
              } else {
                // the group exists so we only add the missing duplicates
                if (groupsFound.size() > 1) {
                  // this should never happen
                  LOG.warn("More than 1 duplicates found for dtos with params: {}", params);
                }
                duplicates.get(groupsFound.get(0)).addAll(duplicatesGroup);
              }
            });

    DuplicatesResult result = new DuplicatesResult();
    result.setDuplicates(new ArrayList<>(duplicates.values()));
    // all the dtos are supposed to have the same generated date
    result.setGenerationDate(dtos.get(0).getGeneratedDate());

    return result;
  }

  private boolean assumeTransitiveClusters(DuplicatesSearchParams params) {
    if (Boolean.TRUE.equals(params.getSameCode()) || Boolean.TRUE.equals(params.getSameName())) {
      return true;
    } else {
      // if uses the fuzzy name it's not transitive
      return !Boolean.TRUE.equals(params.getSameFuzzyName());
    }
  }
}
