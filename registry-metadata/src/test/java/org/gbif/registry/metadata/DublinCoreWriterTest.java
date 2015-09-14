package org.gbif.registry.metadata;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
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
        d.setTitle("This is a keyboard dataset");
        d.setPubDate(new Date());
        d.setDescription("This is the description of the keyboard dataset");

        d.setHomepage(URI.create("http:///www.gbif.org"));

        List<KeywordCollection> listKeyWordColl = Lists.newArrayList();
        KeywordCollection keyword = new KeywordCollection();
        keyword.addKeyword("keyboard");
        keyword.addKeyword("qwerty");
        listKeyWordColl.add(keyword);
        d.setKeywordCollections(listKeyWordColl);

        List<Citation> citationList = Lists.newArrayList();
        Citation citation = new Citation();
        citation.setText("Qwerty U (2015). The World Register of Keyboards. 2015-08-09");
        citationList.add(citation);
        d.setBibliographicCitations(citationList);

        StringWriter writer = new StringWriter();
        DublinCoreWriter.write(d, writer);

        //Load test file
        String expectedContent = FileUtils.readFileToString(org.gbif.utils.file.FileUtils.getClasspathFile("dc/qwerty_dc.xml"));
        //compare without the 'newline' character(s)
        assertEquals(StringUtils.chomp(expectedContent), StringUtils.chomp(writer.toString()));
    }
}