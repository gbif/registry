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
package org.gbif.registry.ws.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import org.slf4j.Marker;

/**
 * Custom event evaluator for DOI SMTP appender filtering.
 * Replaces the removed JaninoEventEvaluator for Logback 1.5+.
 * 
 * This evaluator accepts log events that:
 * - Have a marker containing "DOI_SMTP"
 * - Have a log level of INFO or higher
 */
public class DoiSmtpEventEvaluator extends EventEvaluatorBase<ILoggingEvent> {

  @Override
  public boolean evaluate(ILoggingEvent event) {
    Marker marker = event.getMarker();
    
    // Check if marker exists and contains "DOI_SMTP"
    if (marker != null && marker.contains("DOI_SMTP")) {
      // Check if log level is INFO or higher
      return event.getLevel().isGreaterOrEqual(Level.INFO);
    }
    
    return false;
  }
}
