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
package org.gbif.registry.metasync.protocols.digir.model;

/**
 * We wouldn't need this interface and the two subclasses if it weren't for a bug in Digester.
 *
 * @see <a
 *     href="https://mail-archives.apache.org/mod_mbox/commons-user/201304.mbox/%3CCAD-Ua_g9Y%2BpgvvZzMG8KAefLhdKW5ZJhp3Gwxn2xA99wLTzH%3DA%40mail.gmail.com%3E">Mailing
 *     list post</a>
 */
public interface DigirContact {

  String getName();

  void setName(String name);

  String getTitle();

  void setTitle(String title);

  String getEmail();

  void setEmail(String email);

  String getPhone();

  void setPhone(String phone);

  String getType();

  void setType(String type);
}
