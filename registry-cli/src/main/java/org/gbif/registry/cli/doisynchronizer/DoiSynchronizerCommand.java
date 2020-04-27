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
package org.gbif.registry.cli.doisynchronizer;

import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import static org.gbif.registry.cli.doisynchronizer.DoiSynchronizerConfigurationValidator.isConfigurationValid;

@MetaInfServices(Command.class)
public class DoiSynchronizerCommand extends BaseCommand {

  private final DoiSynchronizerConfiguration config;
  private DoiSynchronizer synchronizer;

  public DoiSynchronizerCommand() {
    super("doi-synchronizer");
    this.config = new DoiSynchronizerConfiguration();
  }

  // constructor for tests
  public DoiSynchronizerCommand(DoiSynchronizerConfiguration config) {
    super("doi-synchronizer");
    this.config = config;
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }

  @Override
  protected void doRun() {
    synchronizer = new DoiSynchronizer(config);
    if (isConfigurationValid(config)) {
      if (StringUtils.isNotBlank(config.doi)) {
        synchronizer.handleDOI();
      } else if (StringUtils.isNotBlank(config.doiList)) {
        synchronizer.handleListDOI();
      } else if (config.listFailedDOI) {
        synchronizer.printFailedDOI();
      }
    }
  }

  public DoiSynchronizer getSynchronizer() {
    return synchronizer;
  }
}
