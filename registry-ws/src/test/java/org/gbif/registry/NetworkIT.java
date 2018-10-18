/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry;

import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Networks;
import org.gbif.registry.ws.resources.NetworkResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.UUID;

/**
 * This is parameterized to run the same test routines for the following:
 * <ol>
 * <li>The persistence layer</li>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class NetworkIT extends NetworkEntityTest<Network> {

  @Parameters
  public static Iterable<Object[]> data() {
    return ImmutableList.<Object[]>of(new Object[] {
      RegistryTestModules.webservice().getInstance(NetworkResource.class),
      null},
      new Object[] {
        RegistryTestModules.webserviceClient().getInstance(NetworkService.class),
        RegistryTestModules.webserviceClient().getInstance(SimplePrincipalProvider.class)
    });
  }

  public NetworkIT(NetworkService service, @Nullable SimplePrincipalProvider pp) {
    super(service, pp);
  }

  @Override
  protected Network newEntity() {
    return Networks.newInstance();
  }

  /**
   * Test doesn't make sense for a network.
   */
  @Override
  public void testCreateAsEditor() {}

  protected Network duplicateForCreateAsEditorTest(Network entity) throws Exception {
    throw new UnsupportedOperationException();
  }

  protected UUID keyForCreateAsEditorTest(Network entity) {
    return null;
  }
}
