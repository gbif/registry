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
package org.gbif.registry.ws.guice;

import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.ws.server.interceptor.StringTrimInterceptor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringTrimInterceptorTest {

  private static final StringTrimInterceptor TRIMMER = new StringTrimInterceptor();

  @Test
  public void test() {
    Dataset dataset = new Dataset();
    dataset.setTitle("   ");
    // TRIMMER.trimStringsOf(dataset);
    assertEquals("Dataset title should be null", null, dataset.getTitle());

    Citation citation = new Citation();
    citation.setText("");
    dataset.setCitation(citation);
    // TRIMMER.trimStringsOf(dataset);
    assertEquals("Citation text should be null", null, dataset.getCitation().getText());
  }
}
