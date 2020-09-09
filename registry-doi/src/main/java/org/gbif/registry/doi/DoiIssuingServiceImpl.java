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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.doi.config.DoiConfigurationProperties;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.persistence.mapper.DoiMapper;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

@Service
public class DoiIssuingServiceImpl implements DoiIssuingService {

  private static final Logger LOG = LoggerFactory.getLogger(DoiIssuingServiceImpl.class);

  private static final int RANDOM_LENGTH = 6;
  private static final String DOI_CHARACTERS = "23456789abcdefghjkmnpqrstuvwxyz"; // Exclude 0o 1il

  private final DoiMapper doiMapper;
  private final String prefix;

  public DoiIssuingServiceImpl(
      DoiMapper doiMapper, DoiConfigurationProperties doiConfigProperties) {
    this.doiMapper = doiMapper;
    prefix = doiConfigProperties.getPrefix();
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with '10.'");
  }

  @Override
  public DOI newDatasetDOI() {
    return newDOI("", DoiType.DATASET);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public DOI newDownloadDOI() {
    return newDOI("dl.", DoiType.DOWNLOAD);
  }

  @Override
  public DOI newDataPackageDOI() {
    return newDOI("dp.", DoiType.DATA_PACKAGE);
  }

  @Override
  public DOI newDerivedDatasetDOI() {
    return newDOI("dd.", DoiType.DERIVED_DATASET);
  }

  private DOI newDOI(final String shoulder, DoiType type) {
    // try a thousand times then fail
    for (int x = 0; x < 1000; x++) {
      DOI doi = random(shoulder);
      try {
        doiMapper.create(doi, type);
        if (x > 100) {
          LOG.warn("Had to search {} times to find the available {} DOI {}.", x, type, doi);
        }
        return doi;
      } catch (Exception e) {
        // might have hit a unique constraint, try another doi
        if (x <= 100) {
          LOG.debug("Random {} DOI {} already exists at attempt {}", type, doi, x);
        } else {
          LOG.info("Random {} DOI {} already exists at attempt {}", type, doi, x);
        }
      }
    }
    throw new IllegalStateException("Tried 1000 random DOIs and none worked, giving up.");
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist
   *     already
   */
  private DOI random(@Nullable String shoulder) {
    String suffix =
        Strings.nullToEmpty(shoulder) + RandomStringUtils.random(RANDOM_LENGTH, DOI_CHARACTERS);
    return new DOI(prefix, suffix);
  }

  @Override
  public boolean isGbif(DOI doi) {
    return doi != null && doi.getPrefix().equalsIgnoreCase(prefix);
  }
}
