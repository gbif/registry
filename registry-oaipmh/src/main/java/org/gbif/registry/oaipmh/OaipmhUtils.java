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

import javax.xml.stream.XMLStreamException;

import org.dspace.xoai.xml.XmlWritable;
import org.dspace.xoai.xml.XmlWriter;

import com.lyncode.xml.exceptions.XmlWriteException;

public final class OaipmhUtils {

  private OaipmhUtils() {}

  /** Add xml header line. */
  protected static String write(final XmlWritable handle)
      throws XMLStreamException, XmlWriteException {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + XmlWriter.toString(writer -> writer.write(handle));
  }
}
