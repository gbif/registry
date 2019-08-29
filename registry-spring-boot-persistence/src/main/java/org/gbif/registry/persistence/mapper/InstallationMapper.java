package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.InstallationType;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Repository
public interface InstallationMapper extends BaseNetworkEntityMapper<Installation> {

  long countInstallationsEndorsedBy(@Param("nodeKey") UUID nodeKey);

  List<Installation> listInstallationsEndorsedBy(@Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page);

  long countInstallationsByOrganization(@Param("organizationKey") UUID organizationKey);

  List<Installation> listInstallationsByOrganization(@Param("organizationKey") UUID organizationKey,
                                                     @Nullable @Param("page") Pageable page);

  List<Installation> deleted(@Nullable @Param("page") Pageable page);

  long countDeleted();

  long countNonPublishing();

  List<Installation> nonPublishing(@Nullable @Param("page") Pageable page);

  /**
   * A simple suggest by title service.
   */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);

  /**
   * Count all installations having all non null filters given.
   */
  int countWithFilter(@Nullable @Param("type") InstallationType type);

  /**
   * Obtains a list of all installations filtered optionally by a type.
   */
  List<Installation> listWithFilter(@Nullable @Param("type") InstallationType type, @Nullable @Param("page") Pageable page);
}
