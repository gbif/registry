package org.gbif.registry.cli.doisynchronizer.diagnostic;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;

import java.net.URI;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Container object that represents the result of the diagnostic of a single DOI.
 *
 * Mutable class, not thread safe.
 */
@NotThreadSafe
public abstract class GbifDOIDiagnosticResult {

  private DOI doi;
  private DoiData doiData;

  private boolean doiExistsAtDatacite;
  private DoiStatus dataciteDoiStatus;
  private URI dataciteTarget;
  private boolean metadataEquals;
  private boolean isCurrentDOI;

  /**
   * Mechanism to let concrete classes provide contextual content to the diagnostic
   * @return
   */
  public abstract List<String> getContextInformation();

  public GbifDOIDiagnosticResult(DOI doi){
    this.doi = doi;
  }

  public DOI getDoi(){
    return doi;
  }

  public DoiData getDoiData() {
    return doiData;
  }

  public void setDoiData(DoiData doiData) {
    this.doiData = doiData;
  }

  public boolean isDoiExistsAtDatacite() {
    return doiExistsAtDatacite;
  }

  public void setDoiExistsAtDatacite(boolean doiExistsAtDatacite) {
    this.doiExistsAtDatacite = doiExistsAtDatacite;
  }

  public boolean isMetadataEquals() {
    return metadataEquals;
  }

  public void setMetadataEquals(boolean metadataEquals) {
    this.metadataEquals = metadataEquals;
  }

  public boolean isCurrentDOI() {
    return isCurrentDOI;
  }

  public void setIsCurrentDOI(boolean isCurrentDOI) {
    this.isCurrentDOI = isCurrentDOI;
  }

  public DoiStatus getDataciteDoiStatus() {
    return dataciteDoiStatus;
  }

  public void setDataciteDoiStatus(DoiStatus dataciteDoiStatus) {
    this.dataciteDoiStatus = dataciteDoiStatus;
  }

  public URI getDataciteTarget() {
    return dataciteTarget;
  }

  public void setDataciteTarget(URI dataciteTarget) {
    this.dataciteTarget = dataciteTarget;
  }
}
