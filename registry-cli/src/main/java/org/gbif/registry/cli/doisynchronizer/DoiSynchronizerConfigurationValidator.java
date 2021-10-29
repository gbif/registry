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
package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DOI;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

public final class DoiSynchronizerConfigurationValidator {

  private DoiSynchronizerConfigurationValidator() {}

  /**
   * Check the current status of the DOI related configurations from the instance of {@link
   * DoiSynchronizerConfiguration}. The method is intended to be used on command line and will print
   * messages using System.out.
   */
  public static boolean isConfigurationValid(DoiSynchronizerConfiguration config) {
    boolean result = true;
    if (config.listFailedDOI
        && (StringUtils.isNotBlank(config.doi)
            || StringUtils.isNotBlank(config.doiList)
            || config.export
            || config.fixDOI)) {
      System.out.println(" --list-failed-doi must be used alone");
      result = false;
    } else if (StringUtils.isNotBlank(config.doi) && StringUtils.isNotBlank(config.doiList)) {
      System.out.println(" --doi and --doi-list can not be used at the same time");
      result = false;
    } else if (config.export && StringUtils.isNotBlank(config.doiList)) {
      System.out.println(" --export can not be used with --doi-list");
      result = false;
    } else if (StringUtils.isNotBlank(config.doi)) {
      if (!DOI.isParsable(config.doi)) {
        System.out.println(config.doi + " is not a valid DOI");
        result = false;
      }
    } else if (StringUtils.isNotBlank(config.doiList)) {
      if (!new File(config.doiList).exists()) {
        System.out.println("DOI list can not be found: " + config.doiList);
        result = false;
      }
    }

    return result;
  }
}
