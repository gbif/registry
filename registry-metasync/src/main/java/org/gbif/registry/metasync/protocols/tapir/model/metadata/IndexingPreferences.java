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
package org.gbif.registry.metasync.protocols.tapir.model.metadata;

import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

@ObjectCreate(pattern = "response/metadata/indexingPreferences")
public class IndexingPreferences {

  private static final Logger LOG = LoggerFactory.getLogger(IndexingPreferences.class);

  @SetProperty(pattern = "response/metadata/indexingPreferences", attributeName = "startTime")
  private String startTime;

  private DateTime parsedStartTime;

  @SetProperty(pattern = "response/metadata/indexingPreferences", attributeName = "maxDuration")
  private Period duration;

  @SetProperty(pattern = "response/metadata/indexingPreferences", attributeName = "frequency")
  private Period frequency;

  public DateTime getParsedStartTime() {
    return parsedStartTime;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
    try {
      parsedStartTime = ISODateTimeFormat.timeParser().parseDateTime(startTime);
    } catch (IllegalFieldValueException ignored) {
      LOG.debug("Could not parse time: [{}]", startTime);
    }
  }

  public Period getDuration() {
    return duration;
  }

  public void setDuration(Period duration) {
    this.duration = duration;
  }

  public Period getFrequency() {
    return frequency;
  }

  public void setFrequency(Period frequency) {
    this.frequency = frequency;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("startTime", parsedStartTime)
        .add("duration", duration)
        .add("frequency", frequency)
        .toString();
  }
}
