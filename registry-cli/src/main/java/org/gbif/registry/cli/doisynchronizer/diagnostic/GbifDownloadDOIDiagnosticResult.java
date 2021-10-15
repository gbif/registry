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
import org.gbif.api.model.occurrence.Download;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

/** Container object that represents the result of the diagnostic of a single Download DOI. */
@NotThreadSafe
public class GbifDownloadDOIDiagnosticResult extends GbifDOIDiagnosticResult {

  private Download download;

  public GbifDownloadDOIDiagnosticResult(DOI doi) {
    super(doi);
  }

  public Download getDownload() {
    return download;
  }

  public void setDownload(Download download) {
    this.download = download;
  }

  @Override
  public List<String> getContextInformation() {
    List<String> contextInformation = new ArrayList<>();

    if (download == null) {
      contextInformation.add("WARNING: No download found");
      return contextInformation;
    }

    contextInformation.add("Download key: " + download.getKey());
    contextInformation.add("Download status: " + download.getStatus());
    return contextInformation;
  }
}
