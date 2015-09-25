package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.MockDatasetMapper;

import java.util.UUID;

import org.dspace.xoai.dataprovider.handlers.results.ListSetsResult;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test class for OaipmhSetRepository
 *
 * @author cgendreau
 */
public class OaipmhSetRepositoryTest {

  private static UUID installationUUIDRepitle = UUID.randomUUID();
  private static UUID installationUUIDGulo = UUID.randomUUID();

  /**
   * Prepare the MockDatasetMapper with controlled mock data.
   *
   * @return
   */
  private static DatasetMapper prepareDatasetMapperMock(){
    MockDatasetMapper datasetMapper = new MockDatasetMapper();
    datasetMapper.mockDatasetForCountry(Country.DENMARK);
    datasetMapper.mockDatasetForCountry(Country.SWEDEN);
    datasetMapper.mockDatasetForCountry(Country.NORWAY);

    Installation installation = new Installation();
    installation.setKey(installationUUIDRepitle);
    installation.setTitle("My reptile IPT");
    datasetMapper.mockDatasetForInstallation(installation);

    installation = new Installation();
    installation.setKey(installationUUIDGulo);
    installation.setTitle("My gulo observations IPT");
    datasetMapper.mockDatasetForInstallation(installation);

    return datasetMapper;
  }

  @Test
  public void testParseSetName(){
    assertTrue(OaipmhSetRepository.parseSetName("country:DK").isPresent());
    assertFalse(OaipmhSetRepository.parseSetName("null:Country").isPresent());
  }

  @Test
  public void testRetrieveSets(){
    DatasetMapper mockDatasetMapper = prepareDatasetMapperMock();
    OaipmhSetRepository setRepository = new OaipmhSetRepository(mockDatasetMapper);

    // the +1 to each list size covers the top level set
    int total = (DatasetType.values().length + 1) + (mockDatasetMapper.listDistinctCountries(null).size() + 1) + (mockDatasetMapper.listDistinctInstallations(null).size() + 1);

    //test retrieving all Sets one by one
    for(int i=0; i < total; i++){
      ListSetsResult result = setRepository.retrieveSets(i, 1);
      assertEquals(1, result.getResults().size());
      assertEquals(i+1 < total, result.hasMore());
    }
  }

  @Test
  public void testSetExists(){
    DatasetMapper mockDatasetMapper = prepareDatasetMapperMock();
    OaipmhSetRepository setRepository = new OaipmhSetRepository(mockDatasetMapper);
    assertTrue("Should find Set provided in test data:" + installationUUIDRepitle.toString(),
            setRepository.exists(OaipmhSetRepository.SetType.INSTALLATION.getSubsetPrefix() + installationUUIDRepitle.toString()));
    assertFalse("Should not find a Set for random Installation key",
            setRepository.exists(OaipmhSetRepository.SetType.INSTALLATION.getSubsetPrefix() + UUID.randomUUID().toString()));

    assertTrue("Should find Set provided in test data:" + Country.DENMARK.getIso2LetterCode(),
            setRepository.exists(OaipmhSetRepository.SetType.COUNTRY.getSubsetPrefix() + Country.DENMARK.getIso2LetterCode()));
    assertFalse("Should not find Set not provided in test data:" + Country.AUSTRALIA.getIso2LetterCode(),
            setRepository.exists(OaipmhSetRepository.SetType.COUNTRY.getSubsetPrefix() + Country.AUSTRALIA.getIso2LetterCode()));
  }

}
