/**
 * 
 */
package org.gbif.registry.util;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.ws.client.guice.SingleUserAuthModule;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tool to loop over all live datasets and issue an unchanged dataset update for all datasets without a DOI
 * so a new GBIF one will get issued.
 */
public class DatasetDOIUpdater {
  private static final String API = "http://api.gbif.org/v1";
  private static final String USER = "crawler.gbif.org";
  private static final String PASSWORD = "";

  private static final Logger LOG = LoggerFactory.getLogger(DatasetDOIUpdater.class);

  public static void main(String[] args) throws Exception {
    Properties p = new Properties();
    p.put("registry.ws.url", API);
    Injector injector = Guice.createInjector(new RegistryWsClientModule(p), new SingleUserAuthModule(USER, PASSWORD));

    LOG.info("Starting test");
    DatasetService ds = injector.getInstance(DatasetService.class);

    PagingRequest req = new PagingRequest(0,100);
    PagingResponse<Dataset> resp = null;
    int cntExist = 0;
    int cntIssued= 0;
    int cntFailed= 0;
    while (resp == null || !resp.isEndOfRecords()) {
      LOG.info("Loading new dataset page {}", req);
      resp = ds.list(req);
      for (Dataset d : resp.getResults()) {
        if (d.getDoi() == null) {
          try {
            ds.update(d);
            cntIssued++;
          } catch (Exception e) {
            LOG.error("Dataset update failed for {}", d.getKey(), e);
            cntFailed++;
          }
        } else {
          cntExist++;
        }
      }
      LOG.info("Dataset DOI existed={}, issued={}, failed={}", cntExist, cntIssued, cntFailed);
      req.nextPage();
    }
    LOG.info("Dataset DOI Update Finished. DOI existed={}, issued={}, failed={}", cntExist, cntIssued, cntFailed);
  }
}
