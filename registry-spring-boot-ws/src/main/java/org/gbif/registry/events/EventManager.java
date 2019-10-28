package org.gbif.registry.events;

public interface EventManager {

  void post(Object object);

  void register(Object object);

  void unregister(Object object);
}
