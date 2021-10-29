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

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yammer.metrics.reporting.GangliaReporter;

/**
 * A configuration class which holds the host and port to connect yammer metrics to a ganglia
 * server.
 */
@SuppressWarnings("PublicField")
public class GangliaConfiguration {

  @Parameter(names = "--ganglia-host")
  public String host;

  @Parameter(names = "--ganglia-port")
  public int port;

  /** Starts the GangliaReporter, pointing to the configured host and port. */
  @JsonIgnore
  public void start() {
    if (host != null && port > 0) {
      GangliaReporter.enable(1, TimeUnit.MINUTES, host, port);
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GangliaConfiguration.class.getSimpleName() + "[", "]")
        .add("host='" + host + "'")
        .add("port=" + port)
        .toString();
  }
}
