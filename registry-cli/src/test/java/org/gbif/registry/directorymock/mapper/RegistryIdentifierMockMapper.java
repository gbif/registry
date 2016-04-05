package org.gbif.registry.directorymock.mapper;

import org.gbif.api.model.registry.Identifier;
import org.gbif.registry.persistence.mapper.IdentifierMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.beust.jcommander.internal.Lists;

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

  public List<Identifier> getCreated(){
    return created;
  }
}
