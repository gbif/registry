/*
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
 * <p>Mutable class, not thread safe.
 */
@NotThreadSafe
public abstract class GbifDOIDiagnosticResult {

  protected DOI doi;
  protected DoiData doiData;

  protected boolean doiExistsAtDatacite;
  protected DoiStatus dataciteDoiStatus;
  protected URI dataciteTarget;
  protected boolean metadataEquals;
  protected String difference;

  /** Mechanism to let concrete classes provide contextual content to the diagnostic */
  public abstract List<String> getContextInformation();

  public GbifDOIDiagnosticResult(DOI doi) {
    this.doi = doi;
  }

  public DOI getDoi() {
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

  public String getDifference() {
    return difference;
  }

  public void setDifference(String difference) {
    this.difference = difference;
  }
}
