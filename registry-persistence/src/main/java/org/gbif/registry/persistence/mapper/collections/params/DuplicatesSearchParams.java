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

  // only for collections
  private Boolean sameInstitutionKey;
  private List<UUID> inInstitutions;
  private List<UUID> notInInstitutions;

  public Boolean getSameName() {
    return sameName;
  }

  public void setSameName(Boolean sameName) {
    this.sameName = sameName;
  }

  public Boolean getSameFuzzyName() {
    return sameFuzzyName;
  }

  public void setSameFuzzyName(Boolean sameFuzzyName) {
    this.sameFuzzyName = sameFuzzyName;
  }

  public Boolean getSameCode() {
    return sameCode;
  }

  public void setSameCode(Boolean sameCode) {
    this.sameCode = sameCode;
  }

  public Boolean getSameCountry() {
    return sameCountry;
  }

  public void setSameCountry(Boolean sameCountry) {
    this.sameCountry = sameCountry;
  }

  public Boolean getSameCity() {
    return sameCity;
  }

  public void setSameCity(Boolean sameCity) {
    this.sameCity = sameCity;
  }

  public List<Country> getInCountries() {
    return inCountries;
  }

  public void setInCountries(List<Country> inCountries) {
    this.inCountries = inCountries;
  }

  public List<Country> getNotInCountries() {
    return notInCountries;
  }

  public void setNotInCountries(List<Country> notInCountries) {
    this.notInCountries = notInCountries;
  }

  public List<UUID> getExcludeKeys() {
    return excludeKeys;
  }

  public void setExcludeKeys(List<UUID> excludeKeys) {
    this.excludeKeys = excludeKeys;
  }

  public Boolean getSameInstitutionKey() {
    return sameInstitutionKey;
  }

  public void setSameInstitutionKey(Boolean sameInstitutionKey) {
    this.sameInstitutionKey = sameInstitutionKey;
  }

  public List<UUID> getInInstitutions() {
    return inInstitutions;
  }

  public void setInInstitutions(List<UUID> inInstitutions) {
    this.inInstitutions = inInstitutions;
  }

  public List<UUID> getNotInInstitutions() {
    return notInInstitutions;
  }

  public void setNotInInstitutions(List<UUID> notInInstitutions) {
    this.notInInstitutions = notInInstitutions;
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

    private Boolean sameInstitutionKey;
    private List<UUID> inInstitutions;
    private List<UUID> notInInstitutions;

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

    public Builder sameInstitutionKey(Boolean sameInstitutionKey) {
      this.sameInstitutionKey = sameInstitutionKey;
      return this;
    }

    public Builder inInstitutions(List<UUID> inInstitutions) {
      this.inInstitutions = inInstitutions;
      return this;
    }

    public Builder notInInstitutions(List<UUID> notInInstitutions) {
      this.notInInstitutions = notInInstitutions;
      return this;
    }

    public DuplicatesSearchParams build() {
      DuplicatesSearchParams params = new DuplicatesSearchParams();
      params.setSameName(sameName);
      params.setSameFuzzyName(sameFuzzyName);
      params.setSameCode(sameCode);
      params.setSameCity(sameCity);
      params.setSameCountry(sameCountry);
      params.setInCountries(inCountries);
      params.setNotInCountries(notInCountries);
      params.setExcludeKeys(excludeKeys);
      params.setSameInstitutionKey(sameInstitutionKey);
      params.setInInstitutions(inInstitutions);
      params.setNotInInstitutions(notInInstitutions);
      return params;
    }
  }
}
