package org.gbif.registry.doi;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.gbif.occurrence.query.TitleLookupServiceImpl;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service("TitleLookupServiceRegistry")
public class TitleLookupServiceRegistry extends TitleLookupServiceImpl {

  private final DatasetMapper datasetMapper;

  /**
   * Creates a lookup instance from an existing jersey client resource pointing to the root of the
   * API.
   *
   * @param apiRoot
   */
  public TitleLookupServiceRegistry(
      @Value("${api.root.url}") String apiRoot, DatasetMapper datasetMapper) {
    super(apiRoot);
    this.datasetMapper = datasetMapper;
  }

  @Override
  public String getDatasetTitle(String datasetKey) {
    try {
      String title = datasetMapper.title(UUID.fromString(datasetKey));

      if (title != null && !title.isEmpty()) {
        return title;
      }
    } catch (Exception e) {
      log.error("Cannot lookup dataset title {}", datasetKey, e);
    }
    return datasetKey;
  }
}
