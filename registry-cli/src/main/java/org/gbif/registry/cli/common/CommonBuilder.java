package org.gbif.registry.cli.common;

import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommonBuilder is used to build objects from the configuration objects
 */
public class CommonBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DataCiteConfiguration.class);

  public static DataCiteService createDataCiteService(DataCiteConfiguration cfg) {
    LOG.debug("Creating DataCite doi service");
    ServiceConfig doiServiceCfg = new ServiceConfig(cfg.username, cfg.password);
    doiServiceCfg.setApi(cfg.api);

    // single thread HttpClient
    // otherwise we should use HttpUtil.newMultithreadedClient(cfg.timeout, cfg.threads, cfg.threads)
    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(cfg.timeout);
    requestBuilder = requestBuilder.setConnectionRequestTimeout(cfg.timeout);
    HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(requestBuilder.build());

    return new DataCiteService(builder.build(), doiServiceCfg);
  }

}
