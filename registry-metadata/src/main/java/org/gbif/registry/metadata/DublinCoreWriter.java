package org.gbif.registry.metadata;

import com.google.common.collect.Maps;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.gbif.api.model.registry.Dataset;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 *
 * Writer to write a Dataset as DublinCore document.
 *
 * @author cgendreau
 */
public class DublinCoreWriter {

    private static final String TEMPLATE_PATH = "/";
    private static final String EML_TEMPLATE = "oai-dc-profile-template/dc-dataset.ftl";
    private static final Configuration FTL = provideFreemarker();

    private DublinCoreWriter() {
        // static utils class
    }

    /**
     * Provides a freemarker template loader. It is configured to access the utf8 templates folder on the classpath.
     */
    private static Configuration provideFreemarker() {
        // load templates from classpath by prefixing /templates
        TemplateLoader tl = new ClassTemplateLoader(DublinCoreWriter.class, TEMPLATE_PATH);

        Configuration fm = new Configuration();
        fm.setDefaultEncoding("utf8");
        fm.setTemplateLoader(tl);

        return fm;
    }

    /**
     * Write a DublinCore file from a Dataset object.
     */
    public static void write(Dataset dataset, Writer writer) throws IOException {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset can't be null");
        }

        Map<String, Object> map = Maps.newHashMap();
        map.put("dataset", dataset);

        try {
            FTL.getTemplate(EML_TEMPLATE).process(map, writer);
        } catch (TemplateException e) {
            throw new IOException("Error while processing the DublinCore freemarker template for dataset " + dataset.getKey(), e);
        }
    }
}
