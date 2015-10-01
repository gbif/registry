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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lyncode.xml.exceptions.XmlWriteException;
import org.dspace.xoai.dataprovider.DataProvider;
import org.dspace.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import org.dspace.xoai.dataprovider.exceptions.BadArgumentException;
import org.dspace.xoai.dataprovider.handlers.ErrorHandler;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.dspace.xoai.dataprovider.parameters.OAIRequest;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.dspace.xoai.model.oaipmh.Request;
import org.dspace.xoai.services.api.DateProvider;
import org.dspace.xoai.services.impl.SimpleResumptionTokenFormat;
import org.dspace.xoai.services.impl.UTCDateProvider;
import org.dspace.xoai.xml.XmlWritable;
import org.dspace.xoai.xml.XmlWriter;
import org.gbif.api.exception.ServiceUnavailableException;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;

import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Identifier;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.MetadataPrefix;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.ResumptionToken;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Verb;

/**
 * An OAI-PMH endpoint, using the XOAI library.
 */
@Path("oaipmh")
@Singleton
public class OaipmhEndpoint {

  private static final TransformerFactory factory = TransformerFactory.newInstance();

  private static final DateProvider dateProvider = new UTCDateProvider();
  private static final ErrorHandler errorsHandler = new ErrorHandler();

  public Transformer xsltTransformer(String xsltFile) {
    try {
      /*
       * TODO: "An object of this class [Transformer] may not be used in multiple threads running concurrently!"
       * Instead, XOAI should accept an immutable Templates object, and call .newTransformer() to get a Transformer.
       * See https://xalan.apache.org/xalan-j/usagepatterns.html#multithreading
       */
      InputStream stream = this.getClass().getClassLoader().getResourceAsStream("org/gbif/registry/oaipmh/"+xsltFile);
      return factory.newTransformer(new StreamSource(stream));
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException("Unable to read XSLT transform "+xsltFile, e);
    }
  }

  private final MetadataFormat OAIDC_METADATA_FORMAT = new MetadataFormat()
          .withPrefix("oai_dc")
          .withNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
          .withSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd")
          .withTransformer(xsltTransformer("dc.xslt"));

  private final MetadataFormat EML_METADATA_FORMAT = new MetadataFormat()
          .withPrefix("eml")
          .withNamespace("eml://ecoinformatics.org/eml-2.1.1")
          .withSchemaLocation("http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd")
          .withTransformer(xsltTransformer("eml.xslt"));

  private Context context = new Context()
          .withMetadataFormat(OAIDC_METADATA_FORMAT)
          .withMetadataFormat(EML_METADATA_FORMAT);

  private RepositoryConfiguration repositoryConfiguration;
  private Repository repository;
  private DataProvider dataProvider;

  @Inject
  public OaipmhEndpoint(ItemRepository itemRepository, SetRepository setRepository) {

    this.repositoryConfiguration = new RepositoryConfiguration()
            .withRepositoryName("GBIF Registry")
            .withAdminEmail("admin@gbif.org")
            .withBaseUrl("http://localhost")
            .withEarliestDate(new Date())
            .withMaxListIdentifiers(1000)
            .withMaxListSets(1000)
            .withMaxListRecords(100)
            .withGranularity(Granularity.Second)
            .withDeleteMethod(DeletedRecord.PERSISTENT)
            .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>");

    this.repository = new Repository()
            .withItemRepository(itemRepository)
            .withSetRepository(setRepository)
            .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
            .withConfiguration(repositoryConfiguration);

    this.dataProvider = new DataProvider(context, repository);
  }

  @GET
  @Produces(MediaType.APPLICATION_XML)
  public InputStream oaipmh(
          @QueryParam("verb") String verb,
          @Nullable @QueryParam("identifier") String identifier,
          @Nullable @QueryParam("metadataPrefix") String metadataPrefix,
          @Nullable @QueryParam("from") String from,
          @Nullable @QueryParam("until") String until,
          @Nullable @QueryParam("set") String set) {

    Date fromDate = null, untilDate = null;
    OAIRequestParametersBuilder reqBuilder = new OAIRequestParametersBuilder()
            .withVerb(verb)
            .withMetadataPrefix(metadataPrefix);

    if(from != null) {
      try {
        fromDate = dateProvider.parse(from);
      } catch (ParseException pEx) {
        return handleOAIRequestBadArgument(reqBuilder.build(), "from=" + from);
      }
    }

    if(until != null){
      try {
        untilDate = dateProvider.parse(from);
      } catch (ParseException pEx) {
        return handleOAIRequestBadArgument(reqBuilder.build(), "until=" + until);
      }
    }

    reqBuilder.withIdentifier(identifier)
            .withFrom(fromDate)
            .withUntil(untilDate)
            .withSet(set);

    return handleOAIRequest(reqBuilder.build());
  }

  private InputStream handleOAIRequest(OAIRequest request) {
    try {
      OAIPMH oaipmh = dataProvider.handle(request);
      return new ByteArrayInputStream(write(oaipmh).getBytes("UTF-8"));

    } catch (Exception e) {
      throw new ServiceUnavailableException("OAI Failed to serialize dataset", e);
    }
  }

  /**
   * Build and generate the response for a BadArgument error code.
   *
   * @param requestParameters origin of the BadArgument error
   * @param errorMessage textual message to report
   * @return
   */
  private InputStream handleOAIRequestBadArgument(OAIRequest requestParameters, String errorMessage) {

    Request request = new Request(repository.getConfiguration().getBaseUrl())
            .withVerbType(requestParameters.get(Verb))
            .withResumptionToken(requestParameters.get(ResumptionToken))
            .withIdentifier(requestParameters.get(Identifier))
            .withMetadataPrefix(requestParameters.get(MetadataPrefix));

    try {
      OAIPMH errorResponse = new OAIPMH()
              .withRequest(request)
              .withResponseDate(dateProvider.now())
              .withError(errorsHandler.handle(new BadArgumentException(errorMessage)));
      return new ByteArrayInputStream(write(errorResponse).getBytes("UTF-8"));

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
