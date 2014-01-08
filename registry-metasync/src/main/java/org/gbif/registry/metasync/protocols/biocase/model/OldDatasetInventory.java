package org.gbif.registry.metasync.protocols.biocase.model;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

/**
 * This is the inventory retrieved by issuing a {@code scan} request used prior to BioCASe 3.4.
 *
 * @see NewDatasetInventory
 */
@ObjectCreate(pattern = "response/content")
public class OldDatasetInventory {

  private List<String> datasets = Lists.newArrayList();

  public List<String> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<String> datasets) {
    this.datasets = datasets;
  }

  @CallMethod(pattern = "response/content/scan/value")
  public void addDataset(
    @CallParam(pattern = "response/content/scan/value") String dataset
  ) {
    datasets.add(dataset);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("datasets", datasets).toString();
  }
}
