package org.gbif.registry.events;

import com.google.common.eventbus.EventBus;
import org.springframework.stereotype.Service;

@Service
public class EventManagerImpl implements EventManager {

  private EventBus eventBus;

  public EventManagerImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void post(Object object) {
    eventBus.post(object);
  }
}
