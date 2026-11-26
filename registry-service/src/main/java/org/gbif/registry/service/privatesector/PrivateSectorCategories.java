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
package org.gbif.registry.service.privatesector;

/** Constants for deriving dataset categories from the publisher's private sector machine tag. */
public final class PrivateSectorCategories {

  /** Namespace of the publisher tag and of the dataset tags written by the sync. */
  public static final String NAMESPACE = "privateSector.gbif.org";

  /** Publisher (organization) machine tag name holding the sector, e.g. "Consulting". */
  public static final String SECTOR_TAG = "sector";

  /** Dataset machine tag name marking a category that the sync added. */
  public static final String AUTO_CATEGORY_TAG = "autoCategory";

  /** Dataset machine tag name marking a category that was removed by hand and must not come back. */
  public static final String EXCLUDED_CATEGORY_TAG = "excludedCategory";

  /** User recorded as creator/modifier of everything the sync writes. */
  public static final String SYNC_USER = "private-sector-category-sync";

  public static final String DATASET_CATEGORY_VOCABULARY = "DatasetCategory";

  /** Parent concept added to every dataset of a business-sector publisher. */
  public static final String BUSINESS_SECTOR = "BusinessSector";

  private PrivateSectorCategories() {}
}
