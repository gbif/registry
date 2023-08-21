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
package org.gbif.registry.domain.ws.util;

/**
 * Class containing constant values used across legacy resources, in answering legacy web service
 * requests (GBRDS/IPT).
 */
public class LegacyResourceConstants {

  /** Empty constructor. */
  LegacyResourceConstants() {}

  // request / response key names
  public static final String KEY_PARAM = "key";
  public static final String ORGANIZATION_KEY_PARAM = "organisationKey";
  public static final String IPT_KEY_PARAM = "iptKey";
  public static final String NAME_PARAM = "name";
  public static final String NAME_LANGUAGE_PARAM = "nameLanguage";
  public static final String DESCRIPTION_PARAM = "description";
  public static final String DESCRIPTION_LANGUAGE_PARAM = "descriptionLanguage";
  public static final String LOGO_URL_PARAM = "logoURL";
  public static final String HOMEPAGE_URL_PARAM = "homepageURL";
  public static final String PRIMARY_CONTACT_NAME_PARAM = "primaryContactName";
  public static final String PRIMARY_CONTACT_EMAIL_PARAM = "primaryContactEmail";
  public static final String PRIMARY_CONTACT_TYPE_PARAM = "primaryContactType";
  public static final String PRIMARY_CONTACT_ADDRESS_PARAM = "primaryContactAddress";
  public static final String PRIMARY_CONTACT_PHONE_PARAM = "primaryContactPhone";
  public static final String PRIMARY_CONTACT_DESCRIPTION_PARAM = "primaryContactDescription";
  public static final String SERVICE_TYPES_PARAM = "serviceTypes";
  public static final String SERVICE_URLS_PARAM = "serviceURLs";
  public static final String WS_PASSWORD_PARAM = "wsPassword";
  public static final String NODE_NAME_PARAM = "nodeName";
  public static final String NODE_KEY_PARAM = "nodeKey";
  public static final String NODE_CONTACT_EMAIL = "nodeContactEmail";
  public static final String RESOURCE_KEY_PARAM = "resourceKey";
  public static final String TYPE_PARAM = "type";
  public static final String TYPE_DESCRIPTION_PARAM = "typeDescription";
  public static final String ACCESS_POINT_URL_PARAM = "accessPointURL";
  public static final String DOI_PARAM = "doi";
  public static final String SUBTYPE_PARAM = "subtype";

  // request / response value names
  public static final String ADMINISTRATIVE_CONTACT_TYPE = "administrative";
  public static final String TECHNICAL_CONTACT_TYPE = "technical";
  public static final String CHECKLIST_SERVICE_TYPE_1 = "DWC-ARCHIVE-CHECKLIST";
  public static final String CHECKLIST_SERVICE_TYPE_2 = "DWC_ARCHIVE_CHECKLIST";
  public static final String OCCURRENCE_SERVICE_TYPE_1 = "DWC-ARCHIVE-OCCURRENCE";
  public static final String OCCURRENCE_SERVICE_TYPE_2 = "DWC_ARCHIVE_OCCURRENCE";
  public static final String MATERIAL_ENTITY_SERVICE_TYPE_1 = "DWC-ARCHIVE-MATERIAL-ENTITY";
  public static final String MATERIAL_ENTITY_SERVICE_TYPE_2 = "DWC_ARCHIVE_MATERIAL_ENTITY";
  public static final String SAMPLING_EVENT_SERVICE_TYPE = "DWC-ARCHIVE-SAMPLING-EVENT";
  public static final String CAMTRAP_DP_SERVICE_TYPE = "CAMTRAP_DP";

  // web service paging request size
  public static final int WS_PAGE_SIZE = 100;
}
