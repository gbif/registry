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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.gbif.registry.cli.doisynchronizer.DoiSynchronizerConfigurationValidator.isConfigurationValid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
public class DoiSynchronizerConfigurationValidatorTest {

  @Test
  public void testDefaultConfigurationValid() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertTrue(result);
  }

  @Test
  public void testConfigurationListFailedMustNotBeUsedWithDoi() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.listFailedDOI = true;
    configuration.doi = "10.21373/abc";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationListFailedMustNotBeUsedWithDoiList() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.listFailedDOI = true;
    configuration.doiList = "10.21373/abc,10.21373/abcd";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationListFailedMustNotBeUsedWithExport() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.listFailedDOI = true;
    configuration.export = true;

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationListFailedMustNotBeUsedWithFixDOI() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.listFailedDOI = true;
    configuration.fixDOI = true;

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationDoiAndDoiListMustNotBeUsedTogether() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.doi = "10.21373/abc";
    configuration.doiList = "10.21373/abc,10.21373/abcd";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationExportAndDoiListMustNotBeUsedTogether() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.export = true;
    configuration.doiList = "10.21373/abc,10.21373/abcd";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationDoiUnparsable() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.doi = "333";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }

  @Test
  public void testConfigurationDoiParsable() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.doi = "10.21373/abc";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertTrue(result);
  }

  @Test
  public void testConfigurationListDoiFileExists() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.doiList = ClassLoader.getSystemClassLoader().getResource("dois.txt").getFile();

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertTrue(result);
  }

  @Test
  public void testConfigurationListDoiFilDoesNoteExist() {
    // given
    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.doiList = "/wrongFile";

    // when
    boolean result = isConfigurationValid(configuration);

    // then
    assertFalse(result);
  }
}
