package org.gbif.registry.ws.model;

import org.gbif.api.vocabulary.IdentifierType;

import javax.annotation.Nullable;

public class NetworkRequestSearchParams {

  private IdentifierType identifierType;
  private String identifier;
  private String machineTagNamespace; // namespace
  private String machineTagName; // name
  private String machineTagValue; // value
  private String q; // query

  @Nullable
  public IdentifierType getIdentifierType() {
    return identifierType;
  }

  public void setIdentifierType(@Nullable IdentifierType identifierType) {
    this.identifierType = identifierType;
  }

  @Nullable
  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(@Nullable String identifier) {
    this.identifier = identifier;
  }

  @Nullable
  public String getMachineTagNamespace() {
    return machineTagNamespace;
  }

  public void setMachineTagNamespace(@Nullable String machineTagNamespace) {
    this.machineTagNamespace = machineTagNamespace;
  }

  @Nullable
  public String getMachineTagName() {
    return machineTagName;
  }

  public void setMachineTagName(@Nullable String machineTagName) {
    this.machineTagName = machineTagName;
  }

  @Nullable
  public String getMachineTagValue() {
    return machineTagValue;
  }

  public void setMachineTagValue(@Nullable String machineTagValue) {
    this.machineTagValue = machineTagValue;
  }

  @Nullable
  public String getQ() {
    return q;
  }

  public void setQ(@Nullable String q) {
    this.q = q;
  }
}
