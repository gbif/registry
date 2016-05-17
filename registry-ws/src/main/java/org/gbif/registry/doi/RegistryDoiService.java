package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.inject.Inject;

/**
 * Simple adapter to expose DoiMapper as DoiService.
 */
public class RegistryDoiService implements DoiService {

  private DoiMapper mapper;

  @Inject
  public RegistryDoiService(DoiMapper mapper){
    this.mapper = mapper;
  }

  @Override
  public DoiData get(DOI doi) {
    return mapper.get(doi);
  }

  @Override
  public List<Map<String, Object>> list(@Nullable DoiStatus status, @Nullable DoiType type, @Nullable Pageable page) {
    return mapper.list(status, type, page);
  }

  @Override
  public String getMetadata(DOI doi) {
    return mapper.getMetadata(doi);
  }

  @Override
  public void create(DOI doi, DoiType type) {
    mapper.create(doi, type);
  }

  @Override
  public void update(DOI doi, DoiData status, String xml) {
    mapper.update(doi, status, xml);
  }

  @Override
  public void delete(DOI doi) {
    mapper.delete(doi);
  }
}
