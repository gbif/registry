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
package org.gbif.registry.search.util;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;
import org.gbif.utils.text.StringUtils;

import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** */
public class DatasetIndexServiceTest {
  static DatasetRealtimeIndexer service;

  @BeforeClass
  public static void init() {
    // TODO: init service
  }

  @Before
  public void initTest() {
    // TODO: init Elastic
  }

  @Test
  public void add() throws Exception {
    Random rnd = new Random();
    for (int i = 1; i < 100; i++) {
      Dataset d = new Dataset();
      d.setKey(UUID.randomUUID());
      d.setType(DatasetType.values()[rnd.nextInt(DatasetType.values().length)]);
      d.setTitle("Title " + i);
      d.setDescription(StringUtils.randomString(50 * i));
      d.setLicense(License.values()[rnd.nextInt(License.values().length)]);
      service.index(d);
    }
  }

  @Test
  public void delete() throws Exception {}
}
