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
package org.gbif.registry.cli.datasetindex;

import java.util.StringJoiner;

import lombok.Data;

@Data
public class ElasticsearchConfig {

  private String hosts;

  private int connectionTimeOut = 120_000;

  private int socketTimeOut = 60_000;

  private int connectionRequestTimeOut = 60_000;

  private int maxRetryTimeOut = 60_000;

  private String index;

  @Override
  public String toString() {
    return new StringJoiner(", ", ElasticsearchConfig.class.getSimpleName() + "[", "]")
        .add("hosts='" + hosts + "'")
        .add("connectionTimeOut='" + connectionTimeOut + "'")
        .add("socketTimeOut='" + socketTimeOut + "'")
        .add("connectionRequestTimeOut='" + connectionRequestTimeOut + "'")
        .add("maxRetryTimeOut=" + maxRetryTimeOut)
        .add("index=" + index)
        .toString();
  }
}
