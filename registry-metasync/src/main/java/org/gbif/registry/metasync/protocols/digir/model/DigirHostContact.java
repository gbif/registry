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
package org.gbif.registry.metasync.protocols.digir.model;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;

import com.google.common.base.Objects;

@ObjectCreate(pattern = "response/content/metadata/provider/host/contact")
public class DigirHostContact implements DigirContact {

  @BeanPropertySetter(pattern = "response/content/metadata/provider/host/contact/name")
  private String name;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/host/contact/title")
  private String title;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/host/contact/emailAddress")
  private String email;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/host/contact/phone")
  private String phone;

  @SetProperty(pattern = "response/content/metadata/provider/host/contact", attributeName = "type")
  private String type;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getPhone() {
    return phone;
  }

  @Override
  public void setPhone(String phone) {
    this.phone = phone;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .add("title", title)
        .add("email", email)
        .add("phone", phone)
        .add("type", type)
        .toString();
  }
}
