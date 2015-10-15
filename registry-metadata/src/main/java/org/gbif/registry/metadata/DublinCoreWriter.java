package org.gbif.registry.metadata;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 *
 * Writer to serialize a Dataset as DublinCore XML document.
 *
 * @author cgendreau
 */
public class DublinCoreWriter {

    private static final String DC_TEMPLATE = "oai-dc-profile-template/dc-dataset.ftl";
    private static final Configuration FTL = DatasetXMLWriterConfigurationProvider.provideFreemarker();

    private DublinCoreWriter() {
        // static utils class
    }

    /**
     * Write a DublinCore document from a Dataset object.
     *
     * @param organization organization who published this dataset, should not be null but nulls are handled.
     * @param dataset non null dataset object
     * @param writer where the output document will go. The writer is not closed by this method.
     * @throws IOException if an error occurs while processing the template
     */
    public static void write(@Nullable Organization organization, @NotNull Dataset dataset, Writer writer) throws IOException {
        Preconditions.checkNotNull(dataset, "Dataset can't be null");
        Map<String, Object> map = Maps.newHashMap();
        map.put("dataset", dataset);
        map.put("contactAdapter", new ContactAdapter(dataset.getContacts()));
        if(organization != null){
            map.put("organization", organization);
        }
        map = ImmutableMap.copyOf(map);
        try {
            FTL.getTemplate(DC_TEMPLATE).process(map, writer);
        } catch (TemplateException e) {
            throw new IOException("Error while processing the DublinCore Freemarker template for dataset " + dataset.getKey(), e);
        }
    }
}
