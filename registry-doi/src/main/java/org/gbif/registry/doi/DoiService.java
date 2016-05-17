package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 *
 */
public interface DoiService {

  DoiData get (DOI doi);
  List<Map<String, Object>> list (@Nullable DoiStatus status, @Nullable DoiType type,
                                  @Nullable Pageable page);
  String getMetadata( DOI doi);
  void create (DOI doi, DoiType type);
  void update (DOI doi, DoiData status, String xml);
  void delete (DOI doi);
}
