/*
 * Copyright 2015 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.oaipmh;

import com.google.inject.Singleton;
import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.dataprovider.DataProvider;
import com.lyncode.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import com.lyncode.xoai.dataprovider.parameters.OAIRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.dataprovider.repository.RepositoryConfiguration;
import com.lyncode.xoai.model.oaipmh.DeletedRecord;
import com.lyncode.xoai.model.oaipmh.OAIPMH;
import com.lyncode.xoai.model.oaipmh.Verb;
import com.lyncode.xoai.services.impl.SimpleResumptionTokenFormat;
import com.lyncode.xoai.xml.XmlWritable;
import com.lyncode.xoai.xml.XmlWriter;
import org.gbif.api.exception.ServiceUnavailableException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import java.io.*;

/**
 * An OAI-PMH endpoint, using the XOAI library.
 */
@Path("oaipmh")
@Singleton
public class OaipmhEndpoint {

  private com.lyncode.xoai.dataprovider.model.Context context = new com.lyncode.xoai.dataprovider.model.Context()
          .withMetadataFormat("xoai", com.lyncode.xoai.dataprovider.model.MetadataFormat.identity());

  private RepositoryConfiguration repositoryConfiguration;
  private Repository repository;
  private DataProvider dataProvider;

  public OaipmhEndpoint() {

    this.repositoryConfiguration = new RepositoryConfiguration()
            .withDefaults()
            .withRepositoryName("GBIF Registry")
            .withAdminEmail("admin@gbif.org")
            .withBaseUrl("http://localhost")
            //.withEarliestDate()
            //.withMaxListIdentifiers()
            //.withMaxListSets()
            //.withMaxListRecords()
            //.withGranularity(Granularity.Second)
            .withDeleteMethod(DeletedRecord.NO) // TODO Not yet implemented
            .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>")
    ;

    this.repository = new Repository()
            .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
            .withConfiguration(repositoryConfiguration);

    this.dataProvider = new DataProvider(context, repository);
  }

  @GET
  @Produces(MediaType.APPLICATION_XML)
  public InputStream oaipmh(@QueryParam("verb") String verb) {

    switch (verb) {
      case "Identify":
        return handle(new OAIRequestParametersBuilder().withVerb(Verb.Type.Identify).build());

      default:
        throw new RuntimeException("Invalid verb"); // TODO Incorrect exception.
    }

  }

  private InputStream handle(OAIRequest request) {
    try {
      OAIPMH oaipmh = dataProvider.handle(request);
      return new ByteArrayInputStream(write(oaipmh).getBytes("UTF-8"));

    } catch (Exception e) {
      throw new ServiceUnavailableException("OAI Failed to serialize dataset", e);
    }
  }

  protected String write(final XmlWritable handle) throws XMLStreamException, XmlWriteException {
    return XmlWriter.toString(new XmlWritable() {
      @Override
      public void write(XmlWriter writer) throws XmlWriteException {
        writer.write(handle);
      }
    });
  }
}
