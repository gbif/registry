package org.gbif.registry.directorymock.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.IdentifierMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import org.apache.ibatis.annotations.Param;

/**
 *
 */
public class RegistryIdentifierMockMapper implements IdentifierMapper {

  private AtomicInteger idGenerator = new AtomicInteger(0);
  private List<Identifier> created = Lists.newArrayList();

  @Override
  public int createIdentifier(Identifier identifier) {
    identifier.setKey(idGenerator.incrementAndGet());
    created.add(identifier);
    return identifier.getKey();
  }

  @Override
  public List<Identifier> list(@Nullable @Param("type") IdentifierType type,
                               @Nullable @Param("identifier") String identifier, @Nullable @Param("page") Pageable page) {
    return null;
  }

  public List<Identifier> getCreated(){
    return created;
  }
}
