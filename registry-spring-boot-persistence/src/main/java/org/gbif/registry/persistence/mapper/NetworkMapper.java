package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Network;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NetworkMapper extends BaseNetworkEntityMapper<Network> {

  int countDatasetsInNetwork(@Param("networkKey") UUID networkKey);

  void addDatasetConstituent(@Param("networkKey") UUID networkKey, @Param("datasetKey") UUID datasetKey);

  void deleteDatasetConstituent(@Param("networkKey") UUID networkKey, @Param("datasetKey") UUID datasetKey);

  /**
   * @return the list of networks a dataset is a constituent of
   */
  List<Network> listByDataset(@Param("datasetKey") UUID datasetKey);
}
