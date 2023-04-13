package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.service.collections.batch.InstitutionBatchHandler;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.springframework.beans.factory.annotation.Autowired;

public class InstitutionBatchHandlerIT extends BaseBatchHandlerIT<Institution> {

  @Autowired
  public InstitutionBatchHandlerIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionService institutionService,
      InstitutionBatchHandler institutionBatchHandler) {
    super(
        simplePrincipalProvider,
        institutionBatchHandler,
        institutionService,
        CollectionEntityType.INSTITUTION);
  }

  @Override
  Institution newEntity() {
    return new Institution();
  }
}
