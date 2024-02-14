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
package org.gbif.registry.doi.util;

import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;

public final class DataCiteConstants {

  private DataCiteConstants() {}

  public static final String ORCID_NAME_IDENTIFIER_SCHEME = "ORCID";
  public static final String CUSTOM_DOWNLOAD_TITLE = "GBIF Custom Occurrence Download";
  public static final String DOWNLOAD_FORMAT = "Compressed and UTF-8 encoded tab delimited file";
  public static final String DOWNLOAD_TITLE = "Occurrence Download";
  public static final String GBIF_PUBLISHER = "The Global Biodiversity Information Facility";
  public static final License DEFAULT_DOWNLOAD_LICENSE = License.CC_BY_NC_4_0;
  public static final String LICENSE_INFO =
      "Data from some individual datasets included in this download may be licensed under less restrictive terms.";
  public static final String ENGLISH = Language.ENGLISH.getIso3LetterCode();
  public static final String DWCA_FORMAT = "Darwin Core Archive";
  public static final String ZIP_FORMAT = "application/zip";
  // https://www.iana.org/assignments/media-types/text/tab-separated-values
  public static final String TSV_FORMAT = "text/tab-separated-values";
  // Not yet a registered type: https://issues.apache.org/jira/browse/AVRO-488
  public static final String AVRO_FORMAT = "Avro";
  // Not yet a registered type: https://issues.apache.org/jira/browse/PARQUET-1889
  public static final String PARQUET_FORMAT = "Parquet"; // "application/vnd.apache.parquet";
  public static final String API_DOWNLOAD_METADATA = "%soccurrence/download/%s";
}
