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
package org.gbif.registry.cli.common;

import java.net.URI;
import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Nullable;

/** A configuration for the DataCite service. */
@SuppressWarnings("PublicField")
public class DataCiteConfiguration {

  @Parameter(names = "--datacite-username")
  @NotNull
  public String username;

  @Parameter(names = "--datacite-password")
  @NotNull
  public String password;

  @Parameter(names = "--datacite-api")
  @Nullable
  public URI api;

  @Parameter(names = "--datacite-threads")
  public int threads = 10;

  @Parameter(names = "--datacite-timeout")
  public int timeout = 20000;

  @Override
  public String toString() {
    return new StringJoiner(", ", DataCiteConfiguration.class.getSimpleName() + "[", "]")
        .add("username='" + username + "'")
        .add("password='" + password + "'")
        .add("api=" + api)
        .add("threads=" + threads)
        .add("timeout=" + timeout)
        .toString();
  }
}
