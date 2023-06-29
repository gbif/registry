package org.gbif.registry.pipelines.issues;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/** Lightweight client for the Github API. */
@Configuration
public class GithubClientConfig {

  @Bean
  public GithubApiService githubApiService(Config config) {
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    OkHttpClient okHttpClient =
        new OkHttpClient.Builder()
            .cache(null)
            .addInterceptor(new BasicAuthInterceptor(config.githubUser, config.githubPassword))
            .build();

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(config.githubWsUrl)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    return retrofit.create(GithubApiService.class);
  }

  @Component
  @Getter
  @Setter
  @ConfigurationProperties(prefix = "pipelines.issues")
  public static class Config {
    @NotEmpty private String githubWsUrl;
    @NotEmpty private String githubUser;
    @NotEmpty private String githubPassword;
  }
}
