package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

// TODO: 26/08/2019 xml mapper has the different name
/**
 * Mapper that perform operations on {@link MetasyncHistory} instances.
 */
@Repository
public interface MetasyncHistoryMapper {

  int count();

  int countByInstallation(@Param("installationKey") UUID installationKey);

  void create(MetasyncHistory metasyncHistory);

  List<MetasyncHistory> list(@Nullable @Param("page") Pageable page);

  List<MetasyncHistory> listByInstallation(@Param("installationKey") UUID installationKey,
                                           @Nullable @Param("page") Pageable page);
}
