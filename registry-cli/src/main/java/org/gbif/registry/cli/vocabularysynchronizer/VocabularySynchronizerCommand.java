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
package org.gbif.registry.cli.vocabularysynchronizer;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

import com.google.common.util.concurrent.Service;

/** This command synchronizes vocabularies and runs post-processors. */
@MetaInfServices(Command.class)
public class VocabularySynchronizerCommand extends ServiceCommand {

  private final VocabularySynchronizerConfiguration config = new VocabularySynchronizerConfiguration();

  public VocabularySynchronizerCommand() {
    super("vocabulary-synchronizer");
  }

  @Override
  protected Service getService() {
    return new VocabularySynchronizerService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
} 