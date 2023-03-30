package org.gbif.registry.ws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;

@Profile("!test")
@Configuration
@EnableAsync
public class AsyncConfig {

  public AsyncConfig() {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
  }

  //  @Bean("threadPoolTaskExecutor")
  //  public TaskExecutor getAsyncExecutor() {
  //    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
  //    executor.setCorePoolSize(20);
  //    executor.setMaxPoolSize(1000);
  //    executor.setWaitForTasksToCompleteOnShutdown(true);
  //    executor.setThreadNamePrefix("Async-");
  //    executor.initialize(); // this is important, otherwise an error is thrown
  //    return new DelegatingSecurityContextAsyncTaskExecutor(executor);
  //  }

  //  @Bean
  //  public DelegatingSecurityContextAsyncTaskExecutor taskExecutor(ThreadPoolTaskExecutor
  // delegate) {
  //    return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
  //  }

  //  @Bean
  //  public DelegatingSecurityContextAsyncTaskExecutor taskExecutor() {
  //    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
  //    executor.setCorePoolSize(20);
  //    executor.setMaxPoolSize(1000);
  //    executor.setWaitForTasksToCompleteOnShutdown(true);
  //    executor.setThreadNamePrefix("Async-");
  //    executor.initialize(); // this is important, otherwise an error is thrown
  //    return new DelegatingSecurityContextAsyncTaskExecutor(executor);
  //  }

}
