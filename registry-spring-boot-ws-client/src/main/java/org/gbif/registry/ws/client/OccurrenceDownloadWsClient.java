package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.ws.client.retrofit.OccurrenceDownloadRetrofitClient;
import retrofit2.Response;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gbif.registry.ws.client.SyncCall.syncCallWithResponse;

// TODO: 25/09/2019 implement BaseNetworkEntityWsClient and its sub clients (Network and so on)
// TODO: 25/09/2019 test all the clients (elastic part should be done first?). There are possible deser problems
// TODO: 25/09/2019 check response first, not just return response.body()
public class OccurrenceDownloadWsClient
    implements OccurrenceDownloadService {

  private final OccurrenceDownloadRetrofitClient client;

  public OccurrenceDownloadWsClient(OccurrenceDownloadRetrofitClient client) {
    this.client = client;
  }

  @Override
  @Nullable
  public Download get(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    final Response<Download> response = syncCallWithResponse(client.get(key));
    return response.body();
  }

  @Override
  public void update(Download download) {
    syncCallWithResponse(client.update(download));
  }

  @Override
  public void create(Download occurrenceDownload) {
    syncCallWithResponse(client.create(occurrenceDownload));
  }

  @Override
  public PagingResponse<Download> list(@Nullable Pageable page, @Nullable Set<Download.Status> status) {
    final List<String> statusList = status != null ?
        status.stream().map(Enum::toString).collect(Collectors.toList()) :
        Collections.emptyList();
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Download>> response = syncCallWithResponse(client.list(statusList, limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<Download> listByUser(String user, Pageable page, Set<Download.Status> status) {
    final List<String> statusList = status != null ?
        status.stream().map(Enum::toString).collect(Collectors.toList()) :
        Collections.emptyList();
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Download>> response =
        syncCallWithResponse(client.listByUser(user, statusList, limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(@NotNull String downloadKey, @Nullable Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<DatasetOccurrenceDownloadUsage>> response =
        syncCallWithResponse(client.listDatasetUsages(downloadKey, limit, offset));

    return response.body();
  }

  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(@Nullable Date fromDate,
                                                                        @Nullable Date toDate,
                                                                        @Nullable Country publishingCountry,
                                                                        @Nullable UUID datasetKey) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
    final String fromDateStr = fromDate != null ? simpleDateFormat.format(fromDate) : null;
    final String toDateStr = toDate != null ? simpleDateFormat.format(toDate) : null;
    final String countryIso2LetterCode = publishingCountry != null ? publishingCountry.getIso2LetterCode() : null;

    final Response<Map<Integer, Map<Integer, Long>>> response =
        syncCallWithResponse(client.getDownloadedRecordsByDataset(fromDateStr, toDateStr, countryIso2LetterCode, datasetKey));

    return response.body();
  }

  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(@Nullable Date fromDate,
                                                                    @Nullable Date toDate,
                                                                    @Nullable Country userCountry) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
    final String fromDateStr = fromDate != null ? simpleDateFormat.format(fromDate) : null;
    final String toDateStr = toDate != null ? simpleDateFormat.format(toDate) : null;
    final String countryIso2LetterCode = userCountry != null ? userCountry.getIso2LetterCode() : null;

    final Response<Map<Integer, Map<Integer, Long>>> response =
        syncCallWithResponse(client.getDownloadsByUserCountry(fromDateStr, toDateStr, countryIso2LetterCode));

    return response.body();
  }

  @Override
  public void createUsages(String downloadKey, Map<UUID, Long> datasetCitations) {
    syncCallWithResponse(client.createUsages(downloadKey, datasetCitations));
  }
}
