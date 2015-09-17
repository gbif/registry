package org.gbif.registry.metadata;

/**
 * Enum of different EML profile versions.
 *
 * @author cgendreau
 */
public enum EMLProfileVersion {

  GBIF_1_0_2("1.0.2"),
  GBIF_1_1("1.1");

  private String version;

  EMLProfileVersion(String version){
    this.version = version;
  }

  /**
   *
   * @return textual representation of the version
   */
  public String getVersion(){
    return version;
  }

}
