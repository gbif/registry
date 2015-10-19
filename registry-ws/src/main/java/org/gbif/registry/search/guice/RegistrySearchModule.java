package org.gbif.registry.search.guice;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.search.DatasetIndexBuilder;
import org.gbif.registry.search.DatasetIndexUpdateListener;
import org.gbif.registry.search.DatasetSearchServiceSolr;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.service.guice.PrivateServiceModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.io.Closer;
import com.google.inject.Exposed;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

/**
 * A guice module that sets up implementation of the search services and gives SOLR providers for search.
 * <p/>
 * The current implementation uses an embedded SOLR index, that uses a SOLR RAM directory. This is satisfactory while
 * indexes remain small, and while we opt to rebuild the index on startup. Future implementations might consider using
 * an external SOLR server, thus this method signatures use abstract SOLR type, and not a concrete implementation to
 * allow for future changes.
 * <p/>
 * <strong>A note on the gbif-common-search SolrModule {@link org.gbif.common.search.inject.SolrModule}:</strong>
 * <p/>
 * The {@link org.gbif.common.search.inject.SolrModule} was considered for use here, but discarded because in its current implementation:
 * <ul>
 * <li>The {@link org.gbif.common.search.solr.builders.EmbeddedServerBuilder} does not support multiple cores, but that is anticipated as a future
 * requirement for this project</li>
 * <li>The {@link org.gbif.common.search.solr.builders.EmbeddedServerBuilder} does not allow someone to supply additional configuration files (it was
 * developed for unit test purposes only)</li>
 * <li>The SOLR server desired requires only 3 lines of code to instantiate. The implementor felt that readability is
 * improved keeping this together</li>
 * <li>{@link org.gbif.common.search.inject.SolrModule} is complex as it provides configuration based declaration (e.g. to support varying
 * deployments. The implementor felt that was an additional learning curve that is unnecessary to put on future
 * developers of this project. That said, should this project require flexible deployment options in the future,
 * developers would be wise to consider adding multi-core support to the {@link org.gbif.common.search.inject.SolrModule}</li>
 * </ul>
 */
public class RegistrySearchModule extends PrivateServiceModule {

  private static final String REGISTRY_PROPERTY_PREFIX = "registry.search.";

  public RegistrySearchModule(Properties properties) {
    super(REGISTRY_PROPERTY_PREFIX, properties);
  }

  @Override
  public void configureService() {
    bind(DatasetSearchService.class).to(DatasetSearchServiceSolr.class).in(Scopes.SINGLETON);
    bind(DatasetService.class).to(DatasetResource.class).in(Scopes.SINGLETON);
    bind(OrganizationService.class).to(OrganizationResource.class).in(Scopes.SINGLETON);
    bind(NodeService.class).to(NodeResource.class).in(Scopes.SINGLETON);
    bind(InstallationService.class).to(InstallationResource.class).in(Scopes.SINGLETON);
    bind(DatasetIndexUpdateListener.class).asEagerSingleton();
    bind(DatasetIndexBuilder.class).in(Scopes.SINGLETON);
    expose(DatasetSearchService.class);
    expose(DatasetIndexUpdateListener.class); // for testing
    expose(OrganizationService.class); // for testing
    expose(InstallationService.class); // for testing
    expose(DatasetService.class); // for testing
    expose(NodeService.class); // for testing

    // 6 email properties:
    // use dev email?
    mapAndExposeBool("mail.devemail.enabled","useDevEmail");
    // smpt host?
    mapAndExpose("mail.smtp.host","smptHost");
    // smpt port?
    mapAndExposeInt("mail.smtp.port","smptPort");
    // dev email address?
    mapAndExpose("mail.devemail","devEmail");
    // cc (helpdesk) email address?
    mapAndExpose("mail.cc","ccEmail");
    // from email address?
    mapAndExpose("mail.from","fromEmail");
    // Expose solr.home
    mapAndExpose("solr.home","solr.home");
  }

  /**
   * Map the String property "fromProperty" to and String named annotation "name".
   */
  private void mapAndExpose(String fromProperty , String name){
    String from = getVerbatimProperties().getProperty(fromProperty);
    bind(String.class).annotatedWith(Names.named(name)).toInstance(from);
    expose(String.class).annotatedWith(Names.named(name));
  }

  /**
   * Map the String property "fromProperty" to and Boolean named annotation "name".
   */
  private void mapAndExposeBool(String fromProperty , String name){
    boolean from = Boolean.valueOf(getVerbatimProperties().getProperty(fromProperty));
    bind(Boolean.class).annotatedWith(Names.named(name)).toInstance(from);
    expose(Boolean.class).annotatedWith(Names.named(name));
  }

  /**
   * Map the String property "fromProperty" to and Integer named annotation "name".
   */
  private void mapAndExposeInt(String fromProperty , String name){
    int from = Integer.valueOf(getVerbatimProperties().getProperty(fromProperty));
    bind(Integer.class).annotatedWith(Names.named(name)).toInstance(from);
    expose(Integer.class).annotatedWith(Names.named(name));
  }

  /**
   * A provider of the SolrServer to use with a dataset search.
   * Named as such, to allow multiple providers (e.g. solr cores) should this be required in the future.
   *
   * @return An embedded solr server that uses the configuration in /solr on the classpath
   * @throws java.net.URISyntaxException If /solr configuration is not found on the classpath
   */
  @Provides
  @Exposed
  @Named("Dataset")
  @Singleton
  public SolrClient datasetSolr(@Named("solr.home") String solrHome) throws URISyntaxException, IOException {
    Path tmpSolrHome = createTempSolrDirectory(solrHome);
    File conf = new File(tmpSolrHome.toFile().getAbsolutePath(), "solr.xml");
    return new EmbeddedSolrServer(CoreContainer.createAndLoad(tmpSolrHome.toFile().getAbsolutePath(), conf), "dataset");
  }

  /**
   * Creates a temporary Solr directory containing all the Solr configuration files.
   */
  private Path createTempSolrDirectory(String solrHome) throws IOException {
    final Path solrHomePath = Paths.get(solrHome);
    final Path datasetCoreHomePath = Paths.get(solrHome,"dataset");
    File solrHomeFile = solrHomePath.toFile();
    if (solrHomeFile.exists()){
      FileUtils.cleanDirectory(solrHomeFile);
    } else {
      solrHomeFile.mkdirs();
    }
    File confDir = new File(datasetCoreHomePath.toFile(),"conf");
    confDir.mkdirs();
    final Path confPath = Paths.get(confDir.toURI());
    exportResource("/solr/solr.xml",solrHomePath);
    exportResource("/solr/dataset/core.properties", datasetCoreHomePath);
    exportResource("/solr/dataset/conf/mapping-FoldToASCII.txt", confPath);
    exportResource("/solr/dataset/conf/mapping-ISOLatin1Accent.txt", confPath);
    exportResource("/solr/dataset/conf/schema.xml", confPath);
    exportResource("/solr/dataset/conf/solrconfig.xml", confPath);
    return solrHomePath;
  }

  /**
   * Export a resource embedded into a Jar file to the local file path.
   *
   * @param resourceName ie.: "/SmartLibrary.dll"
   * @return The path to the exported resource
   * @throws Exception
   */
  private static String exportResource(String resourceName, Path tmpDir) throws IOException {//todo move to Utils
    Closer closer = Closer.create();
    try {
      URL resource = RegistrySearchModule.class.getResource(resourceName);
      File fileResource = new File(resource.getFile());
      InputStream stream = closer.register(resource.openStream());//note that each / is a directory down in the "jar tree" been the jar the root of the tree"
      if(stream == null) {
        throw new IOException("Cannot get resource \"" + resourceName + "\" from Jar file.");
      }
      int readBytes;
      byte[] buffer = new byte[4096];
      OutputStream resStreamOut = closer.register(new FileOutputStream(tmpDir + "/" + fileResource.getName()));
      while ((readBytes = stream.read(buffer)) > 0) {
        resStreamOut.write(buffer, 0, readBytes);
      }
    } catch (IOException ex) {
      throw ex;
    } finally {
      closer.close();
    }
    return tmpDir + resourceName;
  }
}
