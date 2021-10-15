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
package org.gbif.registry.domain.ws;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class used to generate responses for legacy IPT web service API. For example, the register IPT or
 * register dataset responses must indicate the newly registered entity key in the response. </br>
 * JAXB annotations allow the class to be converted into an XML document or JSON
 * response. @XmlElement is used to specify element names that consumers of legacy services expect
 * to find.
 */
@XmlRootElement(name = "iptEntityResponse")
public class IptEntityResponse {

  private String key;

  /** No argument, default constructor needed by JAXB. */
  public IptEntityResponse() {}

  public IptEntityResponse(String key) {
    this.key = key;
  }

  @NotNull
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
