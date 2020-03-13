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
package org.gbif.registry.cli.doisynchronizer.diagnostic;

import org.gbif.api.model.common.DOI;

import java.io.PrintStream;

/** Print a report from a {@link GbifDOIDiagnosticResult} to the defined {@link PrintStream}. */
public class DoiDiagnosticPrinter {

  private PrintStream out;

  public DoiDiagnosticPrinter(PrintStream out) {
    this.out = out;
  }

  public void printReport(GbifDOIDiagnosticResult result) {
    out.println("------ DOI: " + result.getDoi().getDoiName() + "------");
    out.println("DOI Status (GBIF Database): " + result.getDoiData().getStatus());
    out.println("------ Context -----");
    if (result.getContextInformation() != null) {
      for (String line : result.getContextInformation()) {
        out.println(line);
      }
    }
    out.println("------ Datacite -----");
    out.println("DOI found at Datacite?: " + result.isDoiExistsAtDatacite());
    if (result.isDoiExistsAtDatacite()) {
      out.println("DOI Status at Datacite?: " + result.getDataciteDoiStatus());
      out.println("Datacite Metadata equals?: " + result.isMetadataEquals());
      out.println("Datacite target URI: " + result.getDataciteTarget());
    }
    out.println("------------");
  }

  public void printDOIFixAttemptReport(DOI doi, boolean success) {
    out.println("Attempt to fix DOI " + doi.getDoiName() + ":" + (success ? "success" : "failed"));
  }
}
