package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.persistence.mapper.DatasetMapper;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.UUID;
import javax.inject.Singleton;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.dspace.xoai.dataprovider.handlers.results.ListSetsResult;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.repository.SetRepository;

/**
 * WIP, sets should include all distinct countries, installations and dataset types.
 * Created by cgendreau on 15/09/15.
 */
@Singleton
public class OaipmhSetRepository implements SetRepository {

  public static final String COUNTRY_SET_PREFIX = "country";
  public static final String INSTALLATION_SET_PREFIX = "installation";
  public static final String DATASET_TYPE_SET_PREFIX = "dataset_type";
  public static final String SUB_SET_SEPARATOR = ":";

  private static final Set COUNTRY_SET = new Set(COUNTRY_SET_PREFIX).withName("per country");
  private static final Set INSTALLATION_SET = new Set(INSTALLATION_SET_PREFIX).withName("per installation");

  // should we turn it to immutable list after init?
  private static final List<Set> DATASET_TYPE_SET = Lists.newArrayList(new Set(DATASET_TYPE_SET_PREFIX).withName("per dataset type"));
  static{
    for(DatasetType datasetType : DatasetType.values()){
      DATASET_TYPE_SET.add(new Set(DATASET_TYPE_SET_PREFIX + ":" + datasetType.name()).withName(datasetType.name().toLowerCase()));
    }
  }

  private final DatasetMapper datasetMapper;

  @Inject
  public OaipmhSetRepository(DatasetMapper datasetMapper){
    this.datasetMapper = datasetMapper;
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
    for(Country country : countries){
      possibleSet.add( new Set(COUNTRY_SET_PREFIX + ":" + country.getIso2LetterCode()).withName(country.getTitle()));
    }

    // Do we need to pull more elements ?
    if(offset + length >= possibleSet.size() ) {
      //pull distinct installations
      List<Installation> installations = datasetMapper.listDistinctInstallations(null);
      possibleSet.add(INSTALLATION_SET);
      for (Installation installation : installations) {
        possibleSet.add(new Set(INSTALLATION_SET_PREFIX + ":" + installation.getKey().toString()).withName(installation.getTitle()));
      }
    }
    return new ListSetsResult(offset + length < possibleSet.size(), possibleSet.subList(offset, Math.min(offset + length, possibleSet.size())));
  }

  @Override
  public boolean exists(String set) {

    String rootSet = StringUtils.substringBefore(set, SUB_SET_SEPARATOR);
    String subSet = StringUtils.substringAfter(set, SUB_SET_SEPARATOR);

    switch (rootSet){
      case COUNTRY_SET_PREFIX: return handleCountrySetExists(subSet);
      case INSTALLATION_SET_PREFIX: return handleInstallationSetExists(subSet);
      case DATASET_TYPE_SET_PREFIX: return handleDatasetTypeSetExists(subSet);
      default:
        return false;
    }
  }

  private boolean handleDatasetTypeSetExists(String subSet){
    if (StringUtils.isBlank(subSet)){
      return true;
    }

    for(DatasetType datasetType : DatasetType.values()){
      if(datasetType.name().equalsIgnoreCase(subSet)){
        return true;
      }
    }
    return false;
  }

  private boolean handleCountrySetExists(String subSet){
    if (StringUtils.isBlank(subSet)){
      return true;
    }
    Country country = Country.fromIsoCode(subSet);
    //the Country enum will also handle ISO 3 letters so make sure to match the ISO 2 letters code
    if( country == null || !country.getIso2LetterCode().equalsIgnoreCase(subSet)){
      return false;
    }
    long count = datasetMapper.countWithFilter(country, null);
    return count > 0;
  }

  private boolean handleInstallationSetExists(String subSet){
    if (StringUtils.isBlank(subSet)){
      return true;
    }

    UUID uuid = null;
    try {
      uuid = UUID.fromString(subSet);
    }
    catch (IllegalFormatException ifEx){
      return false;
    }

    if( uuid == null || !subSet.equalsIgnoreCase(uuid.toString())){
      return false;
    }

    long count = datasetMapper.countDatasetsByInstallation(uuid);
    return count > 0;
  }
}
