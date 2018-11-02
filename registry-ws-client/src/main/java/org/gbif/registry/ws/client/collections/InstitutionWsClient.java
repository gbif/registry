package org.gbif.registry.ws.client;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.ws.client.guice.RegistryWs;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class InstitutionWsClient extends BaseExtendableColletionEntityClient<Institution>
    implements InstitutionService {

  /**
   * @param resource the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  @Inject
  protected InstitutionWsClient(
      @RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Institution.class, resource.path("institution"), authFilter, GenericTypes.PAGING_INSTITUTION);
  }
}
