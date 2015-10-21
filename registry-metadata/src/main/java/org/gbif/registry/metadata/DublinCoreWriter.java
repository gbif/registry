package org.gbif.registry.metadata;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Writer to serialize a Dataset as DublinCore XML document.
 * Currently using a OAI DC profile.
 *
 * @author cgendreau
 */
public class DublinCoreWriter {

  private static final String DC_TEMPLATE = "oai-dc-profile-template/dc-dataset.ftl";
  private static final Configuration FTL = DatasetXMLWriterConfigurationProvider.provideFreemarker();

  //We should probably use @Named("portal.url") but it would be more appropriate to wait
  //until we turn this class in a non static one.
  private static final String IDENTIFIER_PREFIX = "http://www.gbif.org/dataset/";

  private DublinCoreWriter() {
    // static utils class
  }

  /**
   * Write a DublinCore document from a Dataset object.
   *
   * @param organization organization who published this dataset, should not be null but nulls are handled.
   * @param dataset      non null dataset object
   * @param writer       where the output document will go. The writer is not closed by this method.
   *
   * @throws IOException if an error occurs while processing the template
   */
  public static void write(@Nullable Organization organization, @NotNull Dataset dataset, Writer writer) throws IOException {
    Preconditions.checkNotNull(dataset, "Dataset can't be null");
    Map<String, Object> map = Maps.newHashMap();
    map.put("dataset", dataset);
    map.put("dc", new DcDatasetWrapper(dataset));

    if (organization != null) {
      map.put("organization", organization);
    }
    map = ImmutableMap.copyOf(map);
    try {
      FTL.getTemplate(DC_TEMPLATE).process(map, writer);
    } catch (TemplateException e) {
      throw new IOException("Error while processing the DublinCore Freemarker template for dataset " + dataset.getKey(), e);
    }
  }

  /**
   * This class requires to be public to be used in the Freemarker template.
   */
  public static class DcDatasetWrapper {
    private final Dataset dataset;
    private final ContactAdapter contactAdapter;

    public DcDatasetWrapper(Dataset dataset) {
      this.dataset = dataset;
      this.contactAdapter = new ContactAdapter(dataset.getContacts());
    }

    public Set<String> getCreators() {
      Set<String> creators = Sets.newHashSet();

      List<Contact> filteredContacts = contactAdapter.getFilteredContacts(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT,
              ContactType.METADATA_AUTHOR, ContactType.ORIGINATOR);
      for(Contact contact : filteredContacts){
        creators.add(ContactAdapter.formatContactName(contact));
      }
      return creators;
    }

    public String getIdentifier() {
      return IDENTIFIER_PREFIX + dataset.getKey().toString();
    }

    public Contact getResourceCreator() {
      return contactAdapter.getResourceCreator();
    }
  }
}
