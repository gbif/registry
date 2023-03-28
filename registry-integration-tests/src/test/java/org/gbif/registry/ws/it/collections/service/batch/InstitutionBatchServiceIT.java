package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.service.collections.batch.InstitutionBatchService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.springframework.beans.factory.annotation.Autowired;

public class InstitutionBatchServiceIT extends BaseBatchServiceIT<Institution> {

  @Autowired
  public InstitutionBatchServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionService institutionService,
      InstitutionBatchService institutionBatchService) {
    super(
        simplePrincipalProvider,
        institutionBatchService,
        institutionService,
        CollectionEntityType.INSTITUTION);
  }

  @Override
  Institution newEntity() {
    return new Institution();
  }
}
