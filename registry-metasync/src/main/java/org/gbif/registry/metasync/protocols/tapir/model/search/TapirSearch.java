package org.gbif.registry.metasync.protocols.tapir.model.search;

import com.google.common.base.Objects;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;

/**
 * Class used to represent the response from a TAPIR search request, sent in order to discover the number of records.
 */
@ObjectCreate(pattern = "response/search/summary")
public class TapirSearch {

  @SetProperty(pattern = "response/search/summary", attributeName = "totalMatched")
  private int numberOfRecords;

  /**
   * Get the total number of records for the Dataset behind the endpoint.
   *
   * @return total number of records for the Dataset
   */
  public int getNumberOfRecords() {
    return numberOfRecords;
  }

  public void setNumberOfRecords(int numberOfRecords) {
    this.numberOfRecords = numberOfRecords;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .toString();
  }
}
