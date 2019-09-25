package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.ws.client.retrofit.OrganizationRetrofitClient;
import org.springframework.http.HttpStatus;
import retrofit2.Response;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.gbif.registry.ws.client.SyncCall.syncCallWithResponse;

public class OrganizationWsClient extends BaseNetworkEntityClient<Organization>
    implements OrganizationService {

  private final OrganizationRetrofitClient client;

  public OrganizationWsClient(OrganizationRetrofitClient client) {
    this.client = client;
  }

  @Override
  public PagingResponse<Dataset> hostedDatasets(UUID organizationKey, Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response =
        syncCallWithResponse(client.hostedDatasets(organizationKey, limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<Dataset> publishedDatasets(UUID organizationKey, Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Dataset>> response =
        syncCallWithResponse(client.publishedDataset(organizationKey, limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, @Nullable Pageable page) {
    final Map<String, String> options = new HashMap<>();
    Optional.ofNullable(page).ifPresent(p -> {
      options.put("limit", String.valueOf(p.getLimit()));
      options.put("offset", String.valueOf(p.getOffset()));
    });
    options.put("country", country.getIso2LetterCode());

    final Response<PagingResponse<Organization>> response = syncCallWithResponse(client.list(options));

    return response.body();
  }

  @Override
  public PagingResponse<Installation> installations(UUID organizationKey, Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Installation>> response =
        syncCallWithResponse(client.installations(organizationKey, limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<Organization> listDeleted(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Organization>> response = syncCallWithResponse(client.listDeleted(limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<Organization> listPendingEndorsement(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Organization>> response =
        syncCallWithResponse(client.listPendingEndorsement(limit, offset));

    return response.body();
  }

  @Override
  public PagingResponse<Organization> listNonPublishing(Pageable page) {
    final int limit = page != null ? page.getLimit() : 20;
    final long offset = page != null ? page.getOffset() : 0;

    final Response<PagingResponse<Organization>> response =
        syncCallWithResponse(client.listNonPublishing(limit, offset));

    return response.body();
  }

  @Override
  public boolean confirmEndorsement(@NotNull UUID organizationKey, @NotNull UUID confirmationKey) {
    final Response<Void> response =
        syncCallWithResponse(client.confirmEndorsement(organizationKey, new ConfirmationKeyParameter(confirmationKey)));

    return response.code() == HttpStatus.NO_CONTENT.value();
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String q) {
    final Response<List<KeyTitleResult>> response = syncCallWithResponse(client.suggest(q));

    return response.body();
  }
}
