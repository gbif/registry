package org.gbif.registry.oaipmh;

import com.google.inject.Inject;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.filter.ScopedFilter;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.handlers.results.ListItemsResults;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.metadata.DublinCoreWriter;
import org.gbif.registry.metadata.EMLWriter;
import org.gbif.registry.ws.resources.DatasetResource;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class OaipmhItemRepository implements ItemRepository {

  private static final Logger LOG = LoggerFactory.getLogger(OaipmhItemRepository.class);

  private final DatasetResource datasetResource;

  @Inject
  public OaipmhItemRepository(DatasetResource datasetResource) {
    this.datasetResource = datasetResource;
  }

  @Override
  public Item getItem(String s) throws IdDoesNotExistException, OAIException {

    // the fully augmented dataset
    Dataset dataset = datasetResource.get(UUID.fromString(s));

    if (dataset != null) {
      try {
        /*
         * The XOAI library doesn't provide us with the metadata type (EML / OAI DC), so both must be produced.
         * An XSLT transform pulls out the one that's required.
         * This is ugly, so see https://github.com/DSpace/xoai/issues/31
         */
        StringWriter xml = new StringWriter();

        xml.write("<root>");

        xml.write("<oaidc>\n");
        DublinCoreWriter.write(dataset, xml);
        xml.write("</oaidc>\n");

        xml.write("<eml>\n");
        EMLWriter.write(dataset, xml);
        xml.write("</eml>\n");

        xml.write("</root>\n");

        return new OaipmhItem(dataset, xml.toString());
      } catch (Exception e) {
        throw new ServiceUnavailableException("Failed to serialize dataset " + s + " to DC/EML", e);
      }
    }

    // TODO: How is an invalid id handled?
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, Date date, Date date1) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, String s) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, String s, Date date, Date date1) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, Date date, Date date1) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, String s) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, String s, Date date, Date date1) throws OAIException {
    return null;
  }
}
