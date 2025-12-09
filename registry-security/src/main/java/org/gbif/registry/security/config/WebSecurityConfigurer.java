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
package org.gbif.registry.security.config;

import static org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL;

import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.LegacyAuthorizationFilter;
import org.gbif.registry.security.ResourceNotFoundRequestFilter;
import org.gbif.registry.security.UserRoles;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter;
import org.gbif.registry.security.jwt.JwtRequestFilter;
import org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
public class WebSecurityConfigurer {

  private final ApplicationContext context;

  private final UserDetailsService userDetailsService;

  public WebSecurityConfigurer(
      @Qualifier("registryUserDetailsService") UserDetailsService userDetailsService,
      ApplicationContext context) {
    this.userDetailsService = userDetailsService;
    this.context = context;
  }

  @Bean("actuatorPasswordEncoder")
  public PasswordEncoder actuatorPasswordEncoder() {
    return org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean("actuatorUserDetailsService")
  public UserDetailsService actuatorUserDetailsService(
      @Value("${security.actuatorUser:actuatorAdmin}") String actuatorUser,
      @Value("${security.actuatorSecret:actuatorPassword}") String actuatorSecret) {

    // Add {noop} prefix if password doesn't have any encoder prefix
    if (!actuatorSecret.startsWith("{")) {
      actuatorSecret = "{noop}" + actuatorSecret;
    }

    return new InMemoryUserDetailsManager(
      User.withUsername(actuatorUser)
        .password(actuatorSecret)
        .roles(UserRoles.ACTUATOR_ROLE)
        .build()
    );
  }

  @Bean("actuatorAuthenticationProvider")
  public DaoAuthenticationProvider actuatorAuthenticationProvider(
      @Qualifier("actuatorUserDetailsService") UserDetailsService userDetailsService,
      @Qualifier("actuatorPasswordEncoder") PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
    // Use the default handler provided by Spring Security 6+
    DefaultMethodSecurityExpressionHandler expressionHandler =
      new DefaultMethodSecurityExpressionHandler();

    expressionHandler.setDefaultRolePrefix("");

    return expressionHandler;
  }

  @Bean
  static SecurityContextHolderStrategy securityContextHolderStrategy() {
    SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL);
    return SecurityContextHolder.getContextHolderStrategy();
  }

  @Bean
  public MethodValidationPostProcessor methodValidationPostProcessor() {
    return new MethodValidationPostProcessor();
  }


  @Bean
  public DaoAuthenticationProvider dbAuthenticationProvider() {
    final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

  /**
   * Security filter chain for actuator endpoints.
   * Public: health, prometheus, metrics (for monitoring).
   * Protected: shutdown, heapdump, env, etc. (require ACTUATOR role).
   */
  @Bean
  @org.springframework.core.annotation.Order(1)
  public SecurityFilterChain actuatorSecurityFilterChain(
      HttpSecurity http,
      @Qualifier("actuatorAuthenticationProvider") DaoAuthenticationProvider authProvider) throws Exception {
    http
      .securityMatcher("/actuator/**")
      .authenticationProvider(authProvider)
      .httpBasic(basic -> {})
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(authz -> authz
        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/actuator/metrics/**").permitAll()
        .requestMatchers("/actuator/**").hasRole(UserRoles.ACTUATOR_ROLE)
      );
    return http.build();
  }

  /**
   * Main security filter chain for API endpoints.
   */
  @Bean
  @org.springframework.core.annotation.Order(2)
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/actuator/**")))
      .httpBasic(AbstractHttpConfigurer::disable)
      .csrf(AbstractHttpConfigurer::disable)
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(authz -> authz
        .anyRequest().authenticated()
      );

    // Add filters
    http.addFilterAfter(context.getBean("httpServletRequestWrapperFilter", HttpServletRequestWrapperFilter.class), CsrfFilter.class)
        .addFilterAfter(context.getBean("requestHeaderParamUpdateFilter", RequestHeaderParamUpdateFilter.class), HttpServletRequestWrapperFilter.class)
        .addFilterAfter(context.getBean("identityFilter", IdentityFilter.class), RequestHeaderParamUpdateFilter.class)
        .addFilterAfter(context.getBean("legacyAuthorizationFilter", LegacyAuthorizationFilter.class), IdentityFilter.class)
        .addFilterAfter(context.getBean("appIdentityFilter", AppIdentityFilter.class), LegacyAuthorizationFilter.class)
        .addFilterAfter(context.getBean("jwtRequestFilter", JwtRequestFilter.class), AppIdentityFilter.class)
        .addFilterAfter(context.getBean("authPreCheckCreationRequestFilter", AuthPreCheckCreationRequestFilter.class), JwtRequestFilter.class)
        .addFilterAfter(context.getBean("editorAuthorizationFilter", EditorAuthorizationFilter.class), AuthPreCheckCreationRequestFilter.class)
        .addFilterAfter(context.getBean("grSciCollEditorAuthorizationFilter", GrSciCollEditorAuthorizationFilter.class), EditorAuthorizationFilter.class)
        .addFilterAfter(context.getBean("resourceNotFoundRequestFilter", ResourceNotFoundRequestFilter.class), GrSciCollEditorAuthorizationFilter.class);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new RegistryPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    // CorsFilter only applies this if the origin header is present in the request
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type"));
    configuration.setAllowedOrigins(Collections.singletonList("*"));
    configuration.setAllowedMethods(
        Arrays.asList("HEAD", "GET", "POST", "DELETE", "PUT", "OPTIONS"));
    configuration.setExposedHeaders(
        Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
