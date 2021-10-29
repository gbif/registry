/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.events;

import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

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

  @Override
  public void register(Object object) {
    eventBus.register(object);
  }

  @Override
  public void unregister(Object object) {
    eventBus.unregister(object);
  }
}
