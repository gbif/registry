package org.gbif.identity.mybatis;

import org.gbif.identity.model.Session;

import java.util.List;

public interface SessionMapper {
  void create(Session session);
  List<Session> list(String username);
  void delete(String session);
  void deleteAll(String username);
}
