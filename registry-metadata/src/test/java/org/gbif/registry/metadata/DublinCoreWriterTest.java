package org.gbif.registry.metadata;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Test class for DublinCoreWriter.
 *
 * @author cgendreau
 */
public class DublinCoreWriterTest extends TestCase {

    @Test
    public void testWrite() throws Exception {
        Dataset d = new Dataset();
        d.setKey(UUID.randomUUID());
        d.setDoi(new DOI("10.1234/5679"));
        d.setTitle("This is a test");
        d.setPubDate(new Date());
        d.setDescription("This is the description");

        KeywordCollection k = new KeywordCollection();
        List<KeywordCollection> listKeyWordColl = Lists.newArrayList();
        KeywordCollection keyword = new KeywordCollection();
        keyword.addKeyword("patate");
        keyword.addKeyword("panais");
        listKeyWordColl.add(keyword);
        d.setKeywordCollections(listKeyWordColl);

        StringWriter writer = new StringWriter();
        DublinCoreWriter.write(d, writer);

        System.out.print(writer.toString());
    }
}