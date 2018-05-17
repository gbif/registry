package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsGetClient;

import java.text.SimpleDateFormat;
import java.util.*;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * OccurrenceDownloadService web service client.
 */
public class OccurrenceDownloadWsClient extends BaseWsGetClient<Download, String> implements
  OccurrenceDownloadService {

  private MultivaluedMap<String,String> buildStatusParam(Set<Download.Status> status){
    MultivaluedMap<String,String> statuses = null;
    if(status != null && !status.isEmpty()) {
      statuses = new MultivaluedMapImpl();
      statuses.put("status", Lists.newArrayList(Collections2.transform(status,new Function<Download.Status, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Download.Status input) {
          return input.name();
        }
      })));
    }
    return statuses;
  }

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
  public PagingResponse<Download> list(@Nullable Pageable page, @Nullable Set<Download.Status> status) {
     return get(GenericTypes.PAGING_OCCURRENCE_DOWNLOAD, null, buildStatusParam(status), page);
  }

  @Override
  public PagingResponse<Download> listByUser(String user, Pageable page, Set<Download.Status> status) {
    return get(GenericTypes.PAGING_OCCURRENCE_DOWNLOAD, null, buildStatusParam(status), page,"user", user);
  }

  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
    @NotNull String downloadKey, @Nullable Pageable page
  ) {
    return get(GenericTypes.PAGING_DATASET_OCCURRENCE_DOWNLOAD, page, downloadKey, "datasets");
  }


  @Override
  public Map<Integer,Map<Integer,Long>> getMonthlyStats(@Nullable Date fromDate, @Nullable Date toDate, @Nullable Country country) {
    return statsServiceCall(fromDate, toDate, country, "stats");
  }

  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadRecordsHostedByCountry(@Nullable Date fromDate, @Nullable Date toDate, @Nullable Country country) {
    return statsServiceCall(fromDate, toDate, country, "stats/downloadedRecords");
  }

  private Map<Integer,Map<Integer,Long>> statsServiceCall(@Nullable Date fromDate, @Nullable Date toDate, @Nullable Country country, String path) {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
    Optional.ofNullable(fromDate).ifPresent( d -> params.add("fromDate", simpleDateFormat.format(d)));
    Optional.ofNullable(toDate).ifPresent( d -> params.add("toDate", simpleDateFormat.format(d)));
    return get(GenericTypes.DOWNLOADS_STATS_TYPE, null, params ,null,path);
  }
}
