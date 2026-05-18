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
package org.gbif.registry.ws.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;
import io.micrometer.core.instrument.MeterRegistry;

@Profile("!test")
@Configuration
@EnableAsync
public class AsyncConfig {

  public AsyncConfig() {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
  }

  @Value("${registry.async.corePoolSize:10}")
  private int corePoolSize;

  @Value("${registry.async.maxPoolSize:50}")
  private int maxPoolSize;

  @Value("${registry.async.queueCapacity:500}")
  private int queueCapacity;

  @Bean
  @Qualifier("boundedTaskExecutor")
  public Executor boundedTaskExecutor(@Autowired(required = false) MeterRegistry registry) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("gbif-dataset-async-");
    executor.initialize();

    // register gauges to monitor executor
    ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
    if (registry != null) {
      registry.gauge("registry.async.executor.active", pool, p -> (double) p.getActiveCount());
      registry.gauge("registry.async.executor.poolSize", pool, p -> (double) p.getPoolSize());
      registry.gauge("registry.async.executor.queueSize", pool.getQueue(), q -> (double) q.size());
    }

    return executor;
  }
}
