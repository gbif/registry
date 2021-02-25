package org.gbif.registry.persistence.mapper.collections.params;

import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.UUID;

public class DuplicatesSearchParams {

  private Boolean sameName;
  private Boolean sameFuzzyName;
  private Boolean sameCode;
  private Boolean sameCountry;
  private Boolean sameCity;
  private List<Country> inCountries;
  private List<Country> notInCountries;
  private List<UUID> excludeKeys;

  public Boolean getSameName() {
    return sameName;
  }

  public Boolean getSameFuzzyName() {
    return sameFuzzyName;
  }

  public Boolean getSameCode() {
    return sameCode;
  }

  public Boolean getSameCountry() {
    return sameCountry;
  }

  public Boolean getSameCity() {
    return sameCity;
  }

  public List<Country> getInCountries() {
    return inCountries;
  }

  public List<Country> getNotInCountries() {
    return notInCountries;
  }

  public List<UUID> getExcludeKeys() {
    return excludeKeys;
  }

  private DuplicatesSearchParams(
      Boolean sameName,
      Boolean sameFuzzyName,
      Boolean sameCode,
      Boolean sameCountry,
      Boolean sameCity,
      List<Country> inCountries,
      List<Country> notInCountries,
      List<UUID> excludeKeys) {
    this.sameName = sameName;
    this.sameFuzzyName = sameFuzzyName;
    this.sameCode = sameCode;
    this.sameCountry = sameCountry;
    this.sameCity = sameCity;
    this.inCountries = inCountries;
    this.notInCountries = notInCountries;
    this.excludeKeys = excludeKeys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Boolean sameName;
    private Boolean sameFuzzyName;
    private Boolean sameCode;
    private Boolean sameCountry;
    private Boolean sameCity;
    private List<Country> inCountries;
    private List<Country> notInCountries;
    private List<UUID> excludeKeys;

    public Builder sameName(Boolean sameName) {
      this.sameName = sameName;
      return this;
    }

    public Builder sameFuzzyName(Boolean sameFuzzyName) {
      this.sameFuzzyName = sameFuzzyName;
      return this;
    }

    public Builder sameCode(Boolean sameCode) {
      this.sameCode = sameCode;
      return this;
    }

    public Builder sameCountry(Boolean sameCountry) {
      this.sameCountry = sameCountry;
      return this;
    }

    public Builder sameCity(Boolean sameCity) {
      this.sameCity = sameCity;
      return this;
    }

    public Builder inCountries(List<Country> inCountries) {
      this.inCountries = inCountries;
      return this;
    }

    public Builder notInCountries(List<Country> notInCountries) {
      this.notInCountries = notInCountries;
      return this;
    }

    public Builder excludeKeys(List<UUID> excludeKeys) {
      this.excludeKeys = excludeKeys;
      return this;
    }

    public DuplicatesSearchParams build() {
      return new DuplicatesSearchParams(
          sameName,
          sameFuzzyName,
          sameCode,
          sameCountry,
          sameCity,
          inCountries,
          notInCountries,
          excludeKeys);
    }
  }
}
