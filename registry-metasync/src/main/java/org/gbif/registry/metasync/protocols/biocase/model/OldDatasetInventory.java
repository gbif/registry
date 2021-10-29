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
package org.gbif.registry.metasync.protocols.biocase.model;

import java.util.List;

import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

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
  public void addDataset(@CallParam(pattern = "response/content/scan/value") String dataset) {
    datasets.add(dataset);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("datasets", datasets).toString();
  }
}
