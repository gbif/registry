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
package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.persistence.mapper.DatasetMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.xoai.dataprovider.handlers.results.ListSetsResult;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.repository.SetRepository;

import com.google.common.collect.Lists;

/**
 * Implementation of a XOAI SetRepository for country, installation, dataset_type sets.
 *
 * @author cgendreau
 */
public class OaipmhSetRepository implements SetRepository {

  private static final String SUB_SET_SEPARATOR = ":";

  /** Enum of the type of Set supported by the OaipmhSetRepository. */
  public enum SetType {
    COUNTRY("country"),
    INSTALLATION("installation"),
    DATASET_TYPE("dataset_type");

    private final String name;

    SetType(String name) {
      this.name = name;
    }

    /** Return the name of the Set formatted to be used as a prefix of a subset. */
    public String getSubsetPrefix() {
      return name + SUB_SET_SEPARATOR;
    }

    @Override
    public String toString() {
      return name;
    }

    /**
     * Get a SetType from a string.
     *
     * @return SetType enum constant, never null
     * @throws NullPointerException if str is null
     * @throws IllegalArgumentException if str is not an enum constant of SetType
     */
    public static SetType fromString(String str) {
      if (str == null) {
        throw new NullPointerException("str is null");
      }
      for (SetType setType : SetType.values()) {
        if (setType.name.equalsIgnoreCase(str)) {
          return setType;
        }
      }
      throw new IllegalArgumentException("No enum constant SetType." + str);
    }
  }

  /** Represents the identification of a set including a possible subset. */
  static class SetIdentification {
    private SetType setType;
    private String subSet;

    public SetIdentification(SetType setType, String subSet) {
      this.setType = setType;
      this.subSet = subSet;
    }

    public SetType getSetType() {
      return setType;
    }

    public String getSubSet() {
      return subSet;
    }
  }

  private static final Set COUNTRY_SET =
      new Set(SetType.COUNTRY.toString()).withName("per country");
  private static final Set INSTALLATION_SET =
      new Set(SetType.INSTALLATION.toString()).withName("per installation");

  // should we turn it to immutable list after init?
  private static final List<Set> DATASET_TYPE_SET =
      Lists.newArrayList(new Set(SetType.DATASET_TYPE.toString()).withName("per dataset type"));

  static {
    for (DatasetType datasetType : DatasetType.values()) {
      DATASET_TYPE_SET.add(
          new Set(SetType.DATASET_TYPE.getSubsetPrefix() + datasetType.name())
              .withName(datasetType.name().toLowerCase()));
    }
  }

  private final DatasetMapper datasetMapper;

  public OaipmhSetRepository(DatasetMapper datasetMapper) {
    this.datasetMapper = datasetMapper;
  }

  /**
   * Parse a given setName into a SetIdentification object.
   *
   * @param setName a Set name in the form of set1 or set1:subset1. Nulls are handled and will
   *     return Optional.absent.
   * @return SetIdentification instance or Optional.absent if not Set can be found with the provided
   *     setName
   */
  public static Optional<SetIdentification> parseSetName(String setName) {
    if (StringUtils.isBlank(setName)) {
      return Optional.empty();
    }

    try {
      String rootSet = StringUtils.substringBefore(setName, SUB_SET_SEPARATOR);
      SetType rootSetType = SetType.fromString(rootSet);
      String subSet = StringUtils.substringAfter(setName, SUB_SET_SEPARATOR);
      return Optional.of(new SetIdentification(rootSetType, subSet));
    } catch (IllegalArgumentException iaEx) {
      return Optional.empty();
    }
  }

  @Override
  public boolean supportSets() {
    return true;
  }

  @Override
  public ListSetsResult retrieveSets(int offset, int length) {

    List<Set> possibleSet = Lists.newArrayList(DATASET_TYPE_SET);

    // pull distinct countries
    List<Country> countries = datasetMapper.listDistinctCountries(null);
    possibleSet.add(COUNTRY_SET);
    for (Country country : countries) {
      possibleSet.add(
          new Set(SetType.COUNTRY.getSubsetPrefix() + country.getIso2LetterCode())
              .withName(country.getTitle()));
    }

    // Do we need to pull more elements ?
    if (offset + length >= possibleSet.size()) {
      // pull distinct installations
      List<Installation> installations = datasetMapper.listDistinctInstallations(null);
      possibleSet.add(INSTALLATION_SET);
      for (Installation installation : installations) {
        possibleSet.add(
            new Set(SetType.INSTALLATION.getSubsetPrefix() + installation.getKey().toString())
                .withName(installation.getTitle()));
      }
    }
    return new ListSetsResult(
        offset + length < possibleSet.size(),
        possibleSet.subList(offset, Math.min(offset + length, possibleSet.size())));
  }

  @Override
  public boolean exists(String set) {

    String rootSet = StringUtils.substringBefore(set, SUB_SET_SEPARATOR);
    if (StringUtils.isBlank(rootSet)) {
      return false;
    }

    SetType rootSetType;
    try {
      rootSetType = SetType.fromString(rootSet);
    } catch (IllegalArgumentException iaEx) {
      return false;
    }

    String subSet = StringUtils.substringAfter(set, SUB_SET_SEPARATOR);
    switch (rootSetType) {
      case COUNTRY:
        return handleCountrySetExists(subSet);
      case INSTALLATION:
        return handleInstallationSetExists(subSet);
      case DATASET_TYPE:
        return handleDatasetTypeSetExists(subSet);
      default:
        return false;
    }
  }

  /**
   * Check if a DatasetType exists. If no subSet is provided this method returns true.
   *
   * @return the provided subSet exists or is empty
   */
  private boolean handleDatasetTypeSetExists(String subSet) {
    if (StringUtils.isBlank(subSet)) {
      return true;
    }
    DatasetType datasetType;
    try {
      datasetType = DatasetType.fromString(subSet);
    } catch (IllegalArgumentException iaEx) {
      return false;
    }

    // still needed until POR-2858 is addressed
    return datasetType != null;
  }

  /**
   * Check if a Country ISO 2 letter code exists. If no subSet is provided this method returns true.
   *
   * @return the provided subSet exists or is empty
   */
  private boolean handleCountrySetExists(String subSet) {
    if (StringUtils.isBlank(subSet)) {
      return true;
    }
    Country country = Country.fromIsoCode(subSet);
    // the Country enum will also handle ISO 3 letters so make sure to match the ISO 2 letters code
    if (country == null || !country.getIso2LetterCode().equalsIgnoreCase(subSet)) {
      return false;
    }
    long count = datasetMapper.countWithFilter(country, null);
    return count > 0;
  }

  /**
   * Check if an Installation key exists. If no subSet is provided this method returns true.
   *
   * @return the provided subSet exists or is empty
   */
  private boolean handleInstallationSetExists(String subSet) {
    if (StringUtils.isBlank(subSet)) {
      return true;
    }

    UUID uuid;
    try {
      uuid = UUID.fromString(subSet);
    } catch (IllegalArgumentException iaEx) {
      return false;
    }

    if (!subSet.equalsIgnoreCase(uuid.toString())) {
      return false;
    }

    long count = datasetMapper.countDatasetsByInstallation(uuid);
    return count > 0;
  }
}
