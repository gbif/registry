package org.gbif.registry.cli.doisynchronizer.diagnostic;

import java.io.PrintStream;

/**
 * Print a report from a {@link GbifDOIDiagnosticResult} to the defined {@link PrintStream}.
 */
public class DoiDiagnosticPrinter {

  private PrintStream out;

  public DoiDiagnosticPrinter(PrintStream out){
    this.out = out;
  }

  public void printReport(GbifDOIDiagnosticResult result){
    out.println("------ DOI: " + result.getDoi().getDoiName() + "------");
    out.println("DOI Status (GBIF Database): " + result.getDoiData().getStatus());
    out.println("------ Context -----");
    if(result.getContextInformation() != null){
      for(String line : result.getContextInformation()){
        out.println(line);
      }
    }
    out.println("------ Datacite -----");
    out.println("DOI found at Datacite?: " + result.isDoiExistsAtDatacite());
    if(result.isDoiExistsAtDatacite()) {
      out.println("DOI Status at Datacite?: " + result.getDataciteDoiStatus());
      out.println("Datacite Metadata equals?: " + result.isMetadataEquals());
      out.println("Datacite target URI: " + result.getDataciteTarget());
    }
    out.println("------------");
  }

}
