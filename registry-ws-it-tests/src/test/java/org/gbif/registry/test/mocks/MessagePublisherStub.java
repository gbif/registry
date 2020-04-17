/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.test.mocks;

import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;

import java.io.IOException;

public class MessagePublisherStub implements MessagePublisher {

  @Override
  public void send(Message message) throws IOException {}

  @Override
  public void send(Message message, boolean persistent) throws IOException {}

  @Override
  public void send(Message message, String exchange) throws IOException {}

  @Override
  public void send(Object message, String exchange, String routingKey) throws IOException {}

  @Override
  public void send(Object message, String exchange, String routingKey, boolean persistent)
      throws IOException {}

  @Override
  public void close() {}
}
