/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.pipelines.issues;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/** Lightweight client for the Github API. */
@Configuration
public class GithubClientConfig {

  @Bean
  public GithubApiService githubApiService(IssuesConfig config) {
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    OkHttpClient okHttpClient =
        new OkHttpClient.Builder()
            .cache(null)
            .addInterceptor(
                new BasicAuthInterceptor(config.getGithubUser(), config.getGithubPassword()))
            .build();

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(config.getGithubWsUrl())
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    return retrofit.create(GithubApiService.class);
  }
}
