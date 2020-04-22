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
package org.gbif.registry.metasync.protocols.tapir.model.search;

import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;

import com.google.common.base.Objects;

/**
 * Class used to represent the response from a TAPIR search request, sent in order to discover the
 * number of records.
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
    return Objects.toStringHelper(this).toString();
  }
}
