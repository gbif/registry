package org.gbif.registry.cli.doiupdater;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.datacite.rest.client.configuration.ClientConfiguration;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiHttpException;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.cli.common.CommonBuilder;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.converter.DownloadConverter;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.gbif.api.model.common.DoiStatus.FAILED;
import static org.gbif.api.model.common.DoiStatus.NEW;
import static org.gbif.api.model.common.DoiStatus.REGISTERED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test DoiUpdateListener for different cases.
 */
@RunWith(MockitoJUnitRunner.class)
public class DoiUpdaterListenerIT {

  private static final String PREFIX = "10.21373";
  private static final String SHOULDER = "gbif.";
  private static final int ATTEMPTS = 4;
  private static final URI TEST_TARGET = URI.create("http://www.gbif.org/datasets");

  private static DoiUpdateListener doiUpdateListener;
  private static DoiMapper doiMapper;
  private static DoiService doiService;
  private static DoiService doiServiceSpy;
  private static DoiUpdateListener doiUpdateListenerWithSpyService;

  private static String rawMessage;
  private static org.codehaus.jackson.map.ObjectMapper vanillaObjectMapper;

  @BeforeClass
  public static void setup() throws Exception {
    Properties properties = PropertiesUtil.loadProperties("doiupdater/application.properties");
    Injector injector = Guice.createInjector(new DoiUpdaterListenerIT.DoiUpdaterServiceITModule(properties));
    doiMapper = injector.getInstance(DoiMapper.class);
    doiService = CommonBuilder.createRestJsonApiDataCiteService(getConfig());
    doiServiceSpy = spy(doiService);
    doiUpdateListener = new DoiUpdateListener(doiService, doiMapper, 1000L);
    doiUpdateListenerWithSpyService = new DoiUpdateListener(doiServiceSpy, doiMapper, 1000L);

    vanillaObjectMapper = new org.codehaus.jackson.map.ObjectMapper();
    final byte[] bytes = Files.readAllBytes(Paths.get(ClassLoader.getSystemClassLoader().getResource("doiupdater/test-send-rabbit.json").getFile()));
    rawMessage = new String(bytes);
  }

  private static ClientConfiguration getConfig() throws Exception {
    try (InputStream dc = FileUtils.classpathStream("doiupdater/doi-updater.yaml")) {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      ClientConfiguration cfg = mapper.readValue(dc, ClientConfiguration.class);
      System.out.println(cfg);
      return cfg;
    }
  }

  private DOI newDoi() {
    return new DOI(PREFIX, SHOULDER + System.nanoTime());
  }

  /**
   * Guice module to bind all required injections.
   */
  private static class DoiUpdaterServiceITModule extends AbstractModule {

    private final Properties properties;

    DoiUpdaterServiceITModule(Properties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure() {
      install(new RegistryMyBatisModule(properties));
    }
  }

  // DB status REGISTERED, but missing at DataCite
  @Test
  public void handleMessageDoiWrongDefinedAsRegisteredAndMessageStatusRegisteredShouldCreateAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    // prepare a DOI which is wrongly defined in DB as registered
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    doiMapper.create(doi, DoiType.DOWNLOAD);
    doiMapper.update(doi, new DoiData(REGISTERED, msg.getTarget()), msg.getMetadata());
    assertEquals(new DoiData(REGISTERED, msg.getTarget()), getActualInDb(doi));

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageNewDoiAndMessageStatusRegisteredShouldCreateAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    prepareNewDoi(doi);
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageNewDoiAndMessageStatusReservedShouldCreateAndReserveDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    prepareNewDoi(doi);
    ChangeDoiMessage msg = prepareMessage(doi, "RESERVED");

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(DoiStatus.RESERVED), getActualInDb(doi));
    assertEquals(new DoiData(DoiStatus.RESERVED), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageRegisteredDoiAndMessageStatusDeletedShouldMarkDoiAsDeleted() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "DELETED");
    prepareRegisteredDoi(doi, msg.getTarget(), msg.getMetadata());

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(DoiStatus.DELETED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Ignore
  @Test
  public void handleMessageReservedDoiAndMessageStatusDeletedShouldBeDeletedFromDbAndDataCite() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "DELETED");
    prepareReservedDoi(doi, msg.getTarget(), msg.getMetadata());

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertNull(getActualInDb(doi));
    assertEquals(new DoiData(NEW), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageReservedDoiAndMessageStatusRegisteredShouldUpdateAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareReservedDoi(doi, msg.getTarget(), msg.getMetadata());

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageRegisteredDoiAndMessageStatusRegisteredShouldUpdateAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareRegisteredDoi(doi, URI.create("http://old.url"), msg.getMetadata());

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageFailedDoiAndMessageStatusRegisteredShouldUpdateAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareFailedDoiMissingInDataCite(doi);

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageFailedDoiButRegisteredInDataCiteAndMessageStatusRegisteredShouldUpdateAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareFailedDoiRegisteredInDataCite(doi, msg.getTarget(), msg.getMetadata());

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageRegisteredDoiAndMessageStatusReservedShouldUpdateStatusInDbWithFailed() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "RESERVED");
    prepareRegisteredDoi(doi, msg.getTarget(), msg.getMetadata());

    // when
    doiUpdateListener.handleMessage(msg);

    // then
    assertEquals(new DoiData(FAILED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageDoiServiceRespondedWithHttpErrorShouldUpdateStatusInDbWithFailed() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareNewDoi(doi);

    doThrow(DoiHttpException.class).when(doiServiceSpy).register(any(DOI.class), any(URI.class), anyString());

    // when
    doiUpdateListenerWithSpyService.handleMessage(msg);

    // then
    // should retry 4 times
    verify(doiServiceSpy, times(ATTEMPTS)).register(doi, msg.getTarget(), msg.getMetadata());

    assertEquals(new DoiData(FAILED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(NEW), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageDoiHasTooLongMetadataShouldRegisterDoiAfterTruncatingMetadata() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    String fullXmlMetadata = msg.getMetadata();
    String truncateXmlMetadataWithoutDescription =
      DownloadConverter.truncateDescription(doi, msg.getMetadata(), msg.getTarget());
    String truncatedXmlMetadataWithoutDescriptionAndRelatedIdentifiers =
      DownloadConverter.truncateConstituents(doi, msg.getMetadata(), msg.getTarget());
    prepareNewDoi(doi);

    // first attempt - fail, pretending the response too long, then truncate 'descriptions'
    // second attempt - fail, pretending the response still too long, then truncate 'relatedIdentifiers'
    // third attempt - success, everything is ok
    doThrow(new DoiHttpException(413))
      .doThrow(new DoiHttpException(413))
      .doCallRealMethod()
      .when(doiServiceSpy).register(any(DOI.class), any(URI.class), anyString());

    // when
    doiUpdateListenerWithSpyService.handleMessage(msg);

    // then
    verify(doiServiceSpy).register(doi, TEST_TARGET, fullXmlMetadata);
    verify(doiServiceSpy).register(doi, TEST_TARGET, truncateXmlMetadataWithoutDescription);
    verify(doiServiceSpy).register(doi, TEST_TARGET, truncatedXmlMetadataWithoutDescriptionAndRelatedIdentifiers);

    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageFirstAttemptDoiExceptionShouldRetryAndRegisterDoi() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareNewDoi(doi);

    // pretending the service responded with DoiException only once
    doThrow(new DoiException("some reason"))
      .doCallRealMethod()
      .when(doiServiceSpy).register(any(DOI.class), any(URI.class), anyString());

    // when
    doiUpdateListenerWithSpyService.handleMessage(msg);

    // then
    verify(doiServiceSpy, times(2)).register(doi, TEST_TARGET, msg.getMetadata());

    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, TEST_TARGET), getActualInDataCite(doi));
  }

  @Test
  public void handleMessageDoiExceptionAllAttemptsShouldRetryFourTimesAndMarkAsFailed() throws Exception {
    // given
    final DOI doi = newDoi();
    ChangeDoiMessage msg = prepareMessage(doi, "REGISTERED");
    prepareNewDoi(doi);

    // pretending the service is not working for some reason - DoiException
    doThrow(new DoiException("some reason"))
      .when(doiServiceSpy).register(any(DOI.class), any(URI.class), anyString());

    // when
    doiUpdateListenerWithSpyService.handleMessage(msg);

    // then
    verify(doiServiceSpy, times(ATTEMPTS)).register(doi, TEST_TARGET, msg.getMetadata());

    assertEquals(new DoiData(FAILED, TEST_TARGET), getActualInDb(doi));
    assertEquals(new DoiData(NEW), getActualInDataCite(doi));
  }

  private void prepareFailedDoiMissingInDataCite(DOI doi) {
    doiMapper.create(doi, DoiType.DOWNLOAD);
    doiMapper.update(doi, new DoiData(FAILED), null);
    assertEquals(new DoiData(FAILED), getActualInDb(doi));
  }

  private void prepareFailedDoiRegisteredInDataCite(DOI doi, URI target, String metadata) throws Exception {
    doiMapper.create(doi, DoiType.DOWNLOAD);
    doiMapper.update(doi, new DoiData(FAILED), null);
    doiService.register(doi, target, metadata);
    assertEquals(new DoiData(FAILED), getActualInDb(doi));
    assertEquals(new DoiData(REGISTERED, target), getActualInDataCite(doi));
  }

  private void prepareNewDoi(DOI doi) {
    doiMapper.create(doi, DoiType.DOWNLOAD);
    assertEquals(new DoiData(NEW), getActualInDb(doi));
  }

  private void prepareReservedDoi(DOI doi, URI target, String metadata) throws Exception {
    doiMapper.create(doi, DoiType.DOWNLOAD);
    doiService.reserve(doi, metadata);
    doiMapper.update(doi, new DoiData(DoiStatus.RESERVED, target), metadata);
    assertEquals(new DoiData(DoiStatus.RESERVED), getActualInDataCite(doi));
    assertEquals(new DoiData(DoiStatus.RESERVED, target), getActualInDb(doi));
  }

  private void prepareRegisteredDoi(DOI doi, URI target, String metadata) throws Exception {
    doiMapper.create(doi, DoiType.DOWNLOAD);
    doiService.register(doi, target, metadata);
    doiMapper.update(doi, new DoiData(REGISTERED, target), metadata);
    assertEquals(new DoiData(REGISTERED, target), getActualInDataCite(doi));
    assertEquals(new DoiData(REGISTERED, target), getActualInDb(doi));
  }

  private ChangeDoiMessage prepareMessage(DOI doi, String status) throws IOException {
    return vanillaObjectMapper.readValue(
      rawMessage
        .replace("${doi}", doi.getDoiName())
        .replace("${status}", status),
      ChangeDoiMessage.class);
  }

  private DoiData getActualInDb(DOI doi) {
    return doiMapper.get(doi);
  }

  private DoiData getActualInDataCite(DOI doi) throws Exception {
    return doiService.resolve(doi);
  }
}
