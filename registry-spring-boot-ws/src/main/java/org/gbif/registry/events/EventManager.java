package org.gbif.registry.events;

public interface EventManager {

  void post(Object object);
}
