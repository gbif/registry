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
package org.gbif.registry.metasync.protocols.tapir.model.metadata;

import java.util.Set;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

@ObjectCreate(pattern = "response/metadata/relatedEntity/entity/hasContact")
public class TapirContact {

  private Set<String> roles = Sets.newHashSet();

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/hasContact/VCARD/FN")
  private String fullName;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/hasContact/VCARD/TITLE")
  private String title;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/hasContact/VCARD/TEL")
  private String telephone;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/hasContact/VCARD/EMAIL")
  private String email;

  @CallMethod(pattern = "response/metadata/relatedEntity/entity/hasContact/role")
  public void addRole(
      @CallParam(pattern = "response/metadata/relatedEntity/entity/hasContact/role") String role) {
    roles.add(role);
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTelephone() {
    return telephone;
  }

  public void setTelephone(String telephone) {
    this.telephone = telephone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("roles", roles)
        .add("fullName", fullName)
        .add("title", title)
        .add("telephone", telephone)
        .add("email", email)
        .toString();
  }
}
