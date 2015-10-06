package org.gbif.registry.utils;

import org.gbif.registry.oaipmh.guice.OaipmhModule;

import java.util.Properties;

import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;

/**
 * Simple utility class to build RepositoryConfiguration for OAI-PMH testing.
 *
 * @author cgendreau
 */
public class OaipmhTestConfiguration {

  public static final int MAX_LIST_RECORDS = 2;

  public static RepositoryConfiguration buildTestRepositoryConfiguration(Properties properties) {

    return new RepositoryConfiguration()
            .withRepositoryName("GBIF Test Registry")
            .withAdminEmail(properties.getProperty(OaipmhModule.OAIPMH_ADMIN_EMAIL_PROPERTY))
            .withBaseUrl(properties.getProperty(OaipmhModule.OAIPMH_BASE_URL_PROPERTY))
            .withEarliestDate(OaipmhModule.EARLIEST_DATE)
            .withMaxListIdentifiers(2)
            .withMaxListSets(2)
            .withMaxListRecords(MAX_LIST_RECORDS)
            .withGranularity(Granularity.Second)
            .withDeleteMethod(DeletedRecord.PERSISTENT)
            .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>");
  }
}
