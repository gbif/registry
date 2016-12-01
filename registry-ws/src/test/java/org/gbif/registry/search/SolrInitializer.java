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
package org.gbif.registry.search;

import com.google.common.base.Preconditions;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Rule that will delete everything from SOLR ready for a new test. It is expected to do this before each test by
 * using the following:
 *
 * <pre>
 * @Rule
 * public SolrInitializer = new SolrInitializer(getSolrServer()); // developer required to provide solrServer
 * </pre>
 */
public class SolrInitializer extends ExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(SolrInitializer.class);
  private final SolrClient solrClient;
  private final DatasetIndexService indexService;

  public SolrInitializer(SolrClient solrClient, DatasetIndexService indexService) {
    this.indexService = indexService;
    this.solrClient = solrClient;
  }

  @Override
  protected void before() throws Throwable {
    Preconditions.checkNotNull(solrClient, "SolrServer is required");
    DatasetSearchUpdateUtils.awaitUpdates(indexService);
    LOG.info("Truncating SOLR");
    solrClient.deleteByQuery("*:*");
    solrClient.commit();
    LOG.info("SOLR truncated");
  }
}
