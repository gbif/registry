package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mapper that perform operations on occurrence downloads.
 */
// TODO: 27/08/2019 test xml mapper (association request notificationAddressesAsString)
@Repository
public interface OccurrenceDownloadMapper {

  Download get(@Param("key") String key);

  Download getByDOI(@Param("doi") DOI doi);

  void update(Download entity);

  void create(Download entity);

  List<Download> list(@Nullable @Param("page") Pageable page);

  int count();

  List<Download> listByStatus(@Nullable @Param("page") Pageable page, @Param("status") Set<Download.Status> status);

  int countByStatus(@Param("status") Set<Download.Status> status);

  List<Download> listByUser(@Param("creator") String creator, @Nullable @Param("page") Pageable page, @Param("status") Set<Download.Status> status);

  int countByUser(@Param("creator") String creator, @Param("status") Set<Download.Status> status);


  List<Facet.Count> getDownloadsByUserCountry(@Nullable @Param("fromDate") Date fromDate,
                                              @Nullable @Param("toDate") Date toDate,
                                              @Nullable @Param("userCountry") String userCountry);

  List<Facet.Count> getDownloadedRecordsByDataset(@Nullable @Param("fromDate") Date fromDate,
                                                  @Nullable @Param("toDate") Date toDate,
                                                  @Nullable @Param("publishingCountry") String publishingCountry,
                                                  @Nullable @Param("datasetKey") UUID datasetKey);
}
