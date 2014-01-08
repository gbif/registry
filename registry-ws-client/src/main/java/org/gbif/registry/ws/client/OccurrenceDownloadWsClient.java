package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsGetClient;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * OccurrenceDownloadService web service client.
 */
public class OccurrenceDownloadWsClient extends BaseWsGetClient<Download, String> implements
  OccurrenceDownloadService {

  @Inject
  public OccurrenceDownloadWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Download.class, resource.path("occurrence/download"), authFilter);
  }

  @Override
  public void update(Download download) {
    put(download, download.getKey());
  }

  @Override
  public void create(Download occurrenceDownload) {
    post(occurrenceDownload, "/");
  }


  @Override
  public PagingResponse<Download> list(Pageable page) {
    return get(GenericTypes.PAGING_OCCURRENCE_DOWNLOAD, page);
  }

  @Override
  public PagingResponse<Download> listByUser(String user, Pageable page) {
    return get(GenericTypes.PAGING_OCCURRENCE_DOWNLOAD, page, "user", user);
  }

}
