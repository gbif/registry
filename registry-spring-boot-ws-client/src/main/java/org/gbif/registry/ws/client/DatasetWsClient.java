package org.gbif.registry.ws.client;

import com.google.common.base.Preconditions;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.DatasetProcessStatusService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.ws.client.retrofit.DatasetRetrofitClient;
import org.springframework.http.HttpStatus;
import retrofit2.Response;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.gbif.registry.ws.client.SyncCall.syncCallWithResponse;

public class DatasetWsClient extends BaseNetworkEntityClient<Dataset>
    implements DatasetService, DatasetProcessStatusService {

  private final DatasetRetrofitClient client;

  public DatasetWsClient(DatasetRetrofitClient client) {
    super(client);
    this.client = client;
  }

  @Override
  public InputStream getMetadataDocument(UUID datasetKey) {
    final Response<InputStream> response = syncCallWithResponse(client.getMetadataDocument(datasetKey));

    return checkResponseIsSuccessful(response);
  }

  @Override
  public Metadata insertMetadata(UUID datasetKey, InputStream document) {
    // TODO: 25/09/2019 unclear how to implement
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PagingResponse<Dataset> listByCountry(Country country, @Nullable DatasetType type, @Nullable Pageable page) {
    final Map<String, String> options = new HashMap<>();
    Optional.ofNullable(page).ifPresent(p -> {
      options.put("limit", String.valueOf(p.getLimit()));
      options.put("offset", String.valueOf(p.getOffset()));
    });
    Optional.ofNullable(type).ifPresent(t -> options.put("type", t.toString()));
    options.put("country", country.getIso2LetterCode());

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.list(options));
    return response.body();
  }

  @Override
  public PagingResponse<Dataset> listByType(DatasetType type, @Nullable Pageable page) {
    final Map<String, String> options = new HashMap<>();
    Optional.ofNullable(page).ifPresent(p -> {
      options.put("limit", String.valueOf(p.getLimit()));
      options.put("offset", String.valueOf(p.getOffset()));
    });
    options.put("type", type.toString());

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.list(options));
    return response.body();
  }

  @Override
  public PagingResponse<Dataset> listConstituents(UUID datasetKey, @Nullable Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.listConstituents(datasetKey, limit, offset));
    return response.body();
  }

  @Override
  public PagingResponse<Dataset> listConstituents(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.listConstituents(limit, offset));
    return response.body();
  }

  @Override
  public List<Metadata> listMetadata(UUID datasetKey, @Nullable MetadataType type) {
    final Response<List<Metadata>> response =
        syncCallWithResponse(client.listMetadata(datasetKey, type != null ? type.toString() : null));
    return response.body();
  }

  @Override
  public Metadata getMetadata(int metadataKey) {
    final Response<Metadata> response = syncCallWithResponse(client.getMetadata(metadataKey));
    return response.body();
  }

  @Override
  public InputStream getMetadataDocument(int metadataKey) {
    final Response<InputStream> response = syncCallWithResponse(client.getMetadataDocument(metadataKey));
    return checkResponseIsSuccessful(response);
  }

  @Override
  public void deleteMetadata(int metadataKey) {
    syncCallWithResponse(client.deleteMetadata(metadataKey));
  }

  @Override
  public PagingResponse<Dataset> listDeleted(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.listDeleted(limit, offset));
    return response.body();
  }

  @Override
  public PagingResponse<Dataset> listDuplicates(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.listDuplicates(limit, offset));
    return response.body();
  }

  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.listDatasetsWithNoEndpoint(limit, offset));
    return response.body();
  }

  @Override
  public void createDatasetProcessStatus(DatasetProcessStatus datasetProcessStatus) {
    Preconditions.checkNotNull(datasetProcessStatus.getCrawlJob(), "DatasetProcessStatus needs a crawl job");
    Preconditions.checkNotNull(datasetProcessStatus.getDatasetKey(), "DatasetProcessStatus needs a dataset key");

    syncCallWithResponse(client.createDatasetProcessStatus(datasetProcessStatus.getDatasetKey(),
        datasetProcessStatus));
  }

  @Override
  public void updateDatasetProcessStatus(DatasetProcessStatus datasetProcessStatus) {
    Preconditions.checkNotNull(datasetProcessStatus.getCrawlJob(), "DatasetProcessStatus needs a crawl job");
    Preconditions.checkNotNull(datasetProcessStatus.getDatasetKey(), "DatasetProcessStatus needs a dataset key");

    syncCallWithResponse(client.updateDatasetProcessStatus(datasetProcessStatus.getDatasetKey(),
        datasetProcessStatus.getCrawlJob().getAttempt(),
        datasetProcessStatus));
  }

  @Override
  public DatasetProcessStatus getDatasetProcessStatus(UUID datasetKey, int attempt) {
    final Response<DatasetProcessStatus> response =
        syncCallWithResponse(client.getDatasetProcessStatus(datasetKey, attempt));

    return response.body();
  }

  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<DatasetProcessStatus>> response =
        syncCallWithResponse(client.listDatasetProcessStatus(limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(UUID datasetKey, Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<DatasetProcessStatus>> response =
        syncCallWithResponse(client.listDatasetProcessStatus(datasetKey, limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<DatasetProcessStatus> listAbortedDatasetProcesses(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<DatasetProcessStatus>> response =
        syncCallWithResponse(client.listAbortedDatasetProcesses(limit, offset));

    return response.body();
  }

  @Override
  public List<Network> listNetworks(UUID datasetKey) {
    final Response<List<Network>> response = syncCallWithResponse(client.listNetworks(datasetKey));
    return response.body();
  }

  @Override
  public PagingResponse<Dataset> listByDOI(String doi, Pageable page) {
    Preconditions.checkState(doi.contains("/"), "DOI must contain one slash");
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final String[] splitDoi = doi.split("/");
    final String doiPrefix = splitDoi[0];
    final String doiSuffix = splitDoi[1];

    final Response<PagingResponse<Dataset>> response = syncCallWithResponse(client.listByDOI(doiPrefix, doiSuffix, limit, offset));
    return response.body();
  }

  private InputStream checkResponseIsSuccessful(Response<InputStream> response) {
    if (response.code() == HttpStatus.NOT_FOUND.value()) {
      return null;
    }

    // TODO: 25/09/2019 add reason phrase
    if (response.code() != HttpStatus.OK.value()) {
      throw new ServiceUnavailableException("HTTP " + response.code());
    }

    return response.body();
  }
}
