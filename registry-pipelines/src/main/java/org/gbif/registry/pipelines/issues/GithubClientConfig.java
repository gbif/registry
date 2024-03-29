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

import feign.Feign;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/** Lightweight client for the GitHub API. */
@Configuration
public class GithubClientConfig {

  @Bean
  public Encoder feignFormEncoder() {
    return new SpringFormEncoder(new SpringEncoder(messageConverters()));
  }

  @Bean
  public GithubApiClient githubApiClient(IssuesConfig config) {

    return Feign.builder()
        .encoder(feignFormEncoder())
        .target(GithubApiClient.class, config.getGithubWsUrl());
  }

  @Bean
  public ObjectFactory<HttpMessageConverters> messageConverters() {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    return () -> new HttpMessageConverters(converters);
  }
}
