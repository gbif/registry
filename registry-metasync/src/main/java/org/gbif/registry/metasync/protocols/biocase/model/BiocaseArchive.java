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
package org.gbif.registry.metasync.protocols.biocase.model;

import java.net.URI;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;
import org.joda.time.DateTime;

import com.google.common.base.Objects;

/**
 * Archive in either ABCD or DwC-A form. To distinguish between the two look at the namespace which
 * should be either ABCD or DwC.
 */
@ObjectCreate(pattern = "inventory/datasets/dataset/archives/archive")
public class BiocaseArchive {

  @SetProperty(pattern = "inventory/datasets/dataset/archives/archive", attributeName = "modified")
  private DateTime modified;

  @SetProperty(pattern = "inventory/datasets/dataset/archives/archive", attributeName = "fileSize")
  private int fileSize;

  @SetProperty(pattern = "inventory/datasets/dataset/archives/archive", attributeName = "rcount")
  private int rcount;

  @SetProperty(pattern = "inventory/datasets/dataset/archives/archive", attributeName = "rowType")
  private URI rowType;

  @SetProperty(pattern = "inventory/datasets/dataset/archives/archive", attributeName = "namespace")
  private URI namespace;

  @BeanPropertySetter(pattern = "inventory/datasets/dataset/archives/archive")
  private URI archiveUrl;

  public DateTime getModified() {
    return modified;
  }

  public void setModified(DateTime modified) {
    this.modified = modified;
  }

  public int getFileSize() {
    return fileSize;
  }

  public void setFileSize(int fileSize) {
    this.fileSize = fileSize;
  }

  public int getRcount() {
    return rcount;
  }

  public void setRcount(int rcount) {
    this.rcount = rcount;
  }

  public URI getRowType() {
    return rowType;
  }

  public void setRowType(URI rowType) {
    this.rowType = rowType;
  }

  public URI getNamespace() {
    return namespace;
  }

  public void setNamespace(URI namespace) {
    this.namespace = namespace;
  }

  public URI getArchiveUrl() {
    return archiveUrl;
  }

  public void setArchiveUrl(URI archiveUrl) {
    this.archiveUrl = archiveUrl;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("modified", modified)
        .add("fileSize", fileSize)
        .add("rcount", rcount)
        .add("rowType", rowType)
        .add("namespace", namespace)
        .add("archiveUrl", archiveUrl)
        .toString();
  }
}
