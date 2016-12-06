/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.grizzly;

import org.gbif.registry.guice.TestRegistryWsServletListener;
import org.gbif.registry.search.DatasetIndexService;

import java.io.IOException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.servlet.GuiceFilter;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An embedded web server using Grizzly that run the registry ws application.
 * To allow WS tests to flush SOLR between runs, this allows the registration of the SOLR server.
 */
public class RegistryServer implements TestRule {

  /**
   * The system property one can set to override the port.
   */
  public static final String PORT_PROPERTY = "grizzly.port"; // to override as a system property

  /**
   * The default port to use, should no system property be supplied.
   */
  public static final int DEFAULT_PORT = 7001;

  private static final Logger LOG = LoggerFactory.getLogger(RegistryServer.class);
  private final GrizzlyWebServer webServer;

  public static final RegistryServer INSTANCE = new RegistryServer();

  private SolrClient solrClient;
  private DatasetIndexService indexService;

  /**
   * Gets the port that grizzly will run on. This will either be {@link #DEFAULT_PORT} or the value supplied as a
   * system
   * property named {@link #PORT_PROPERTY}.
   */
  public static int getPort() {
    String port = System.getProperty(PORT_PROPERTY);
    try {
      return (port == null) ? DEFAULT_PORT : Integer.parseInt(port);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(PORT_PROPERTY + " does not hold a valid port: " + port);
    }
  }

  // we are a singleton instance
  private RegistryServer() {
    webServer = new GrizzlyWebServer(getPort());
    ServletAdapter sa = new ServletAdapter();
    sa.addServletListener(TestRegistryWsServletListener.class.getName());
    sa.addFilter(new GuiceFilter(), "Guice", null);
    sa.setContextPath("/");
    sa.setServletPath("/");
    webServer.addGrizzlyAdapter(sa, null);
  }

  public void start() {
    LOG.info("Starting registry WS for tests");
    try {
      webServer.start();
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  public void stop() {
    webServer.stop();
    LOG.info("Stopping test registry WS");
  }

  public synchronized void setSolrClient(SolrClient solrClient) {
    this.solrClient = solrClient;
  }

  public synchronized void setIndexService(DatasetIndexService indexService) {
    this.indexService = indexService;
  }

  /**
   * Utility to allow this to be used as a rule that will start and stop a server around the statement base.
   */
  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        RegistryServer server = new RegistryServer();
        server.start();
        try {
          base.evaluate();
        } finally {
          server.stop();
        }
      }
    };
  }

  public SolrClient getSolrClient() {
    Preconditions.checkNotNull(solrClient, "Misuse of class. SolrClient has not been set");
    return solrClient;
  }

  public DatasetIndexService getIndexService() {
    Preconditions.checkNotNull(indexService, "Misuse of class. DatasetIndexService has not been set");
    return indexService;
  }
}
