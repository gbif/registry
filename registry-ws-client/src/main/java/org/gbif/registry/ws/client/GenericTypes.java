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
package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sun.jersey.api.client.GenericType;

/**
 * Package access utility to provide generic types.
 */
public class GenericTypes {

  public static final GenericType<PagingResponse<Node>> PAGING_NODE = new GenericType<PagingResponse<Node>>() {
  };
  public static final GenericType<PagingResponse<Organization>> PAGING_ORGANIZATION =
    new GenericType<PagingResponse<Organization>>() {
    };
  public static final GenericType<PagingResponse<Installation>> PAGING_INSTALLATION =
    new GenericType<PagingResponse<Installation>>() {
    };
  public static final GenericType<PagingResponse<Dataset>> PAGING_DATASET = new GenericType<PagingResponse<Dataset>>() {
  };
  public static final GenericType<PagingResponse<Network>> PAGING_NETWORK = new GenericType<PagingResponse<Network>>() {
  };
  public static final GenericType<List<Network>> LIST_NETWORK = new GenericType<List<Network>>() {
  };
  public static final GenericType<List<KeyTitleResult>> LIST_KEY_TITLE = new GenericType<List<KeyTitleResult>>() {
  };
  public static final GenericType<List<Contact>> LIST_CONTACT = new GenericType<List<Contact>>() {
  };
  public static final GenericType<List<Endpoint>> LIST_ENDPOINT = new GenericType<List<Endpoint>>() {
  };
  public static final GenericType<List<MachineTag>> LIST_MACHINETAG = new GenericType<List<MachineTag>>() {
  };
  public static final GenericType<List<Tag>> LIST_TAG = new GenericType<List<Tag>>() {
  };
  public static final GenericType<List<Identifier>> LIST_IDENTIFIER = new GenericType<List<Identifier>>() {
  };
  public static final GenericType<List<Comment>> LIST_COMMENT = new GenericType<List<Comment>>() {
  };
  public static final GenericType<List<Country>> LIST_COUNTRY = new GenericType<List<Country>>() {
  };
  public static final GenericType<List<Metadata>> LIST_METADATA = new GenericType<List<Metadata>>() {
  };
  public static final GenericType<Metadata> METADATA = new GenericType<Metadata>() {
  };
  public static final GenericType<DatasetProcessStatus> DATASET_PROCESS_STATUS =
    new GenericType<DatasetProcessStatus>() {
    };
  public static final GenericType<PagingResponse<Download>> PAGING_OCCURRENCE_DOWNLOAD =
    new GenericType<PagingResponse<Download>>() {
    };
  public static final GenericType<PagingResponse<DatasetOccurrenceDownloadUsage>> PAGING_DATASET_OCCURRENCE_DOWNLOAD =
    new GenericType<PagingResponse<DatasetOccurrenceDownloadUsage>>() {
    };
  public static final GenericType<PagingResponse<DatasetProcessStatus>> PAGING_DATASET_PROCESS_STATUS =
    new GenericType<PagingResponse<DatasetProcessStatus>>() {
    };
  public static final GenericType<PagingResponse<MetasyncHistory>> METASYNC_HISTORY =
    new GenericType<PagingResponse<MetasyncHistory>>() {
    };
  public static final GenericType<Map<UUID, String>> TITLES_MAP_TYPE = new GenericType<Map<UUID, String>>() {
  };

  public static final GenericType<Map<Integer, Map<Integer,Long>>> DOWNLOADS_STATS_TYPE = new GenericType<Map<Integer, Map<Integer,Long>>>() {
  };

  private GenericTypes() {
    throw new UnsupportedOperationException("Can't initialize class");
  }

}
