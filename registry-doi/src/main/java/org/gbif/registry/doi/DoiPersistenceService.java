package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * DOI data access object.
 */
public interface DoiPersistenceService {

  DoiData get(DOI doi);

  String getMetadata(DOI doi);

  List<Map<String, Object>> list(@Nullable DoiStatus status, @Nullable DoiType type,
                                 @Nullable Pageable page);

  void create(DOI doi, DoiType type);

  void update(DOI doi, DoiData status, String xml);

  void delete(DOI doi);
}
