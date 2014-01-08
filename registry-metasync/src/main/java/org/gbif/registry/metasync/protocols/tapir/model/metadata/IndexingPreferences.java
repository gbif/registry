package org.gbif.registry.metasync.protocols.tapir.model.metadata;

import com.google.common.base.Objects;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
