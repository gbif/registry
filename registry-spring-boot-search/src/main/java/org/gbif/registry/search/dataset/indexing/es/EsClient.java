package org.gbif.registry.search.dataset.indexing.es;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Generic ElasticSearch wrapper client to encapsulate indexing and admin operations.
 */
@Component
public class EsClient implements Closeable {

  private RestHighLevelClient restHighLevelClient;

  @Autowired
  public EsClient(@Value("${esHosts}") String hosts) {
    restHighLevelClient = provideEsClient(hosts.split(","));
  }

  /**
   * Points the indexName to the alias, and deletes all the indices that were pointing to the alias.
   */
  public void swapAlias(String alias, String indexName) {
    try {
      GetAliasesRequest getAliasesRequest = new GetAliasesRequest();
      getAliasesRequest.aliases(alias);
      GetAliasesResponse getAliasesResponse = restHighLevelClient.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT);
      Set<String> idxsToDelete = getAliasesResponse.getAliases().keySet();
      IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
      indicesAliasesRequest.addAliasAction(IndicesAliasesRequest.AliasActions.add().alias(alias).index(indexName));
      restHighLevelClient.indices().updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT);
      if (!idxsToDelete.isEmpty()) {
        idxsToDelete.forEach(idx -> indicesAliasesRequest.addAliasAction(IndicesAliasesRequest.AliasActions.remove().index(idx).alias(alias)));
        restHighLevelClient.indices()
          .delete(new DeleteIndexRequest().indices(idxsToDelete.toArray(new String[0])), RequestOptions.DEFAULT);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Creates a new index using the indexName, recordType and settings provided.
   */
  public void createIndex(String indexName, String recordType,  Map<?,?> settings, String mappingFile) {
    try {
      CreateIndexRequest createIndexRequest = new CreateIndexRequest();
      createIndexRequest.index(indexName).settings(settings).mapping(recordType,
                                                                     new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(mappingFile).toURI()))),
                                                                     XContentType.JSON);
      restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    } catch (IOException| URISyntaxException ex)  {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Updates the settings of an existing index.
   */
  public void updateSettings(String indexName, Map<?,?> settings) {
    try {
      UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest();
      updateSettingsRequest.indices(indexName).settings(settings);
      restHighLevelClient.indices().putSettings(updateSettingsRequest, RequestOptions.DEFAULT);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Performs a ElasticSearch {@link BulkRequest}.
   */
  public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
    return restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
  }

  /**
   * Creates ElasticSearch client using default connection settings.
   */
  public static RestHighLevelClient provideEsClient(String[]hostsUrl) {
    HttpHost[] hosts = new HttpHost[hostsUrl.length];
    int i = 0;
    for (String host : hostsUrl) {
      try {
        URL url = new URL(host);
        hosts[i] = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        i++;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    return new RestHighLevelClient(RestClient.builder(hosts));
  }

  @Bean
  public RestHighLevelClient provideRestHighLevelClient(@Value("${esHosts}") String hosts) {
    return provideEsClient(hosts.split(","));
  }

  @Override
  public void close() {
    //shuts down the ES client
    if(Objects.nonNull(restHighLevelClient)) {
      try {
        restHighLevelClient.close();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
