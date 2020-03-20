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
package org.gbif.registry.cli.doisynchronizer;

import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;

public class PostalServiceConfiguration {

  @Parameter(names = "--postalservice-hostname")
  @NotNull
  public String hostname;

  @Parameter(names = "--postalservice-port")
  @NotNull
  public int port;

  @Parameter(names = "--postalservice-username")
  @NotNull
  public String username;

  @Parameter(names = "--postalservice-password")
  @NotNull
  public String password;

  @Parameter(names = "--postalservice-virtualhost")
  @NotNull
  public String virtualhost;

  @Parameter(names = "--postalservice-threadcount")
  @NotNull
  public int threadcount;

  @Override
  public String toString() {
    return new StringJoiner(", ", PostalServiceConfiguration.class.getSimpleName() + "[", "]")
        .add("hostname='" + hostname + "'")
        .add("port=" + port)
        .add("username='" + username + "'")
        .add("password='" + password + "'")
        .add("virtualhost='" + virtualhost + "'")
        .add("threadcount=" + threadcount)
        .toString();
  }
}
