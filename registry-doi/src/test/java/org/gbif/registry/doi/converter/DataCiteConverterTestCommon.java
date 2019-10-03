package org.gbif.registry.doi.converter;

import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.utils.file.FileUtils;

class DataCiteConverterTestCommon {

  static String getXmlMetadataFromFile(String fileName) throws Exception {
    DataCiteMetadata metadata = DataCiteValidator.fromXml(FileUtils.classpathStream(fileName));
    return DataCiteValidator.toXml(metadata, true);
  }
}
