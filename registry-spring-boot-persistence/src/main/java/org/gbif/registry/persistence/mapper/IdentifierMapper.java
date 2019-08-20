package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.IdentifierType;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;

@Repository
public interface IdentifierMapper {

  int createIdentifier(Identifier identifier);

  List<Identifier> list(@Nullable @Param("type") IdentifierType type,
                        @Nullable @Param("identifier") String identifier, @Nullable @Param("page") Pageable page);
}
