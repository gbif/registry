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
package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Dataset;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.model.oaipmh.About;
import org.dspace.xoai.model.oaipmh.Metadata;

public class OaipmhItem implements Item {

  private Dataset dataset;
  private Metadata metadata;
  private List<Set> sets;

  /**
   * Creates a new OaipmhItem instance with no metadata content. Mostly to be used as
   * ItemIdentifier.
   */
  public OaipmhItem(Dataset dataset, List<Set> sets) {
    this(dataset, null, sets);
  }

  public OaipmhItem(Dataset dataset, String metadata, List<Set> sets) {
    this.dataset = dataset;
    this.sets = sets;

    if (metadata != null) {
      this.metadata = new Metadata(metadata);
    }
  }

  @Override
  public List<About> getAbout() {
    return new ArrayList<>();
  }

  @Override
  public Metadata getMetadata() {
    return metadata;
  }

  @Override
  public String getIdentifier() {
    return dataset.getKey().toString();
  }

  @Override
  public Date getDatestamp() {
    return dataset.getModified();
  }

  @Override
  public List<Set> getSets() {
    return sets;
  }

  @Override
  public boolean isDeleted() {
    return dataset.getDeleted() != null;
  }
}
