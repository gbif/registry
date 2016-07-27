package org.gbif.registry.cli.common;

import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.yammer.metrics.reporting.GangliaReporter;

/**
 * A configuration class which holds the host and port to connect yammer metrics to a ganglia server.
 */
@SuppressWarnings("PublicField")
public class GangliaConfiguration {

  @Parameter(names = "--ganglia-host")
  public String host;

  @Parameter(names = "--ganglia-port")
  public int port;

  /**
   * Starts the GangliaReporter, pointing to the configured host and port.
   */
  @JsonIgnore
  public void start() {
    if (host != null && port > 0) {
      GangliaReporter.enable(1, TimeUnit.MINUTES, host, port);
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("host", host).add("port", port).toString();
  }
}
