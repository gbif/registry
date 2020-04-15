package org.gbif.registry.search.dataset.indexing.ws;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Configuration
public class GbifApiServiceConfig {

  @Bean
  public GbifApiService gbifApiService(
    @Value("${api.root.url}") String apiBaseUrl,
    @Qualifier("apiMapper") ObjectMapper objectMapper) {

    OkHttpClient okHttpClient =
      new OkHttpClient()
        .newBuilder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build();
    Retrofit retrofit =
      new Retrofit.Builder()
        .baseUrl(apiBaseUrl)
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .client(okHttpClient)
        .build();
    return retrofit.create(GbifApiService.class);
  }
}
