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
package org.gbif.registry.metasync.protocols.biocase.model;

import java.net.URI;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * This object represents the data received when doing an inventory request using the new
 * functionality as of BioCASe 3.4.
 *
 * @see <a
 *     href="http://wiki.bgbm.org/bps/index.php/VersionHistory#Version_3.4_.5B2013-02-20.5D">BioCASe
 *     Version History</a>
 */
@ObjectCreate(pattern = "inventory")
public class NewDatasetInventory {

  @BeanPropertySetter(pattern = "inventory/status")
  private String status;

  @BeanPropertySetter(pattern = "inventory/service_url")
  private URI serviceUrl;

  private List<InventoryDataset> datasets = Lists.newArrayList();

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public URI getServiceUrl() {
    return serviceUrl;
  }

  public void setServiceUrl(URI serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  public List<InventoryDataset> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<InventoryDataset> datasets) {
    this.datasets = datasets;
  }

  @SetNext
  public void addDataset(InventoryDataset dataset) {
    datasets.add(dataset);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("status", status)
        .add("serviceUrl", serviceUrl)
        .add("datasets", datasets)
        .toString();
  }
}
