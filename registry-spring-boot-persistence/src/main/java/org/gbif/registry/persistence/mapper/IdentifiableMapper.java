package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Identifier;

import java.util.List;
import java.util.UUID;

public interface IdentifiableMapper {

  int addIdentifier(@Param("targetEntityKey") UUID entityKey, @Param("identifierKey") int identifierKey);

  int deleteIdentifier(@Param("targetEntityKey") UUID entityKey, @Param("identifierKey") int identifierKey);

  List<Identifier> listIdentifiers(@Param("targetEntityKey") UUID identifierKey);

}
