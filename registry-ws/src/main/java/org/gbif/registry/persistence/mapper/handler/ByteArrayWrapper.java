package org.gbif.registry.persistence.mapper.handler;

/**
 * We want to retrieve byte arrays from postgres on their own, but there is no way to assign a specific type handler
 * to a simple response. Using this wrapper class in the mybatis mappers we can assign a byte array type handler
 * to the data property.
 */
public class ByteArrayWrapper {
  private byte[] data;

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
