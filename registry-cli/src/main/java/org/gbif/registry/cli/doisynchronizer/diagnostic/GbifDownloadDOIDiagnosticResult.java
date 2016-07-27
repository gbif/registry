package org.gbif.registry.cli.doisynchronizer.diagnostic;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.occurrence.Download;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.Lists;

/**
 * Container object that represents the result of the diagnostic of a single Download DOI.
 */
@NotThreadSafe
public class GbifDownloadDOIDiagnosticResult extends GbifDOIDiagnosticResult {

  private Download download;

  public GbifDownloadDOIDiagnosticResult(DOI doi){
    super(doi);
  }

  public Download getDownload() {
    return download;
  }

  public void setDownload(Download download) {
    this.download = download;
  }

  public List<String> getContextInformation(){
    List<String> contextInformation = Lists.newArrayList();

    if(download == null){
      contextInformation.add("WARNING: No download found");
      return contextInformation;
    }

    contextInformation.add("Download key: " + download.getKey());
    contextInformation.add("Download status: " + download.getStatus());
    return contextInformation;
  }
}
