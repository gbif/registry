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
package org.gbif.registry.cli.common;

import java.util.Properties;

import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;

/** Holds configuration for the registry or identity database. */
@SuppressWarnings("PublicField")
public class DirectoryConfiguration {

  @NotNull
  @Parameter(names = "--directory-ws-url")
  public String wsUrl;

  @NotNull
  @Parameter(names = "--directory-app-key")
  public String appKey;

  @NotNull
  @Parameter(names = "--directory-app-secret")
  public String appSecret;

  /** Create a Properties object from the public fields from that class and its included db. */
  public Properties toProperties() {
    Properties props = new Properties();
    props.put("directory.ws.url", wsUrl);
    props.put("directory.app.key", appKey);
    props.put("directory.app.secret", appSecret);
    return props;
  }
}
