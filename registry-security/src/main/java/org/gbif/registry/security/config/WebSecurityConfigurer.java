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

import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.LegacyAuthorizationFilter;
import org.gbif.registry.security.ResourceNotFoundRequestFilter;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter;
import org.gbif.registry.security.jwt.JwtRequestFilter;
import org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import static org.gbif.ws.security.SecurityUtils.corsAllOriginsAndMethodsConfiguration;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer {

  private final ApplicationContext context;

  private final UserDetailsService userDetailsService;

  public WebSecurityConfigurer(
      @Qualifier("registryUserDetailsService") UserDetailsService userDetailsService,
      ApplicationContext context) {
    this.userDetailsService = userDetailsService;
    this.context = context;
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(dbAuthenticationProvider());
  }

  private DaoAuthenticationProvider dbAuthenticationProvider() {
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

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.httpBasic(AbstractHttpConfigurer::disable)
        .addFilterAfter(context.getBean(HttpServletRequestWrapperFilter.class), CsrfFilter.class)
        .addFilterAfter(
            context.getBean(RequestHeaderParamUpdateFilter.class),
            HttpServletRequestWrapperFilter.class)
        .addFilterAfter(context.getBean(IdentityFilter.class), RequestHeaderParamUpdateFilter.class)
        .addFilterAfter(context.getBean(LegacyAuthorizationFilter.class), IdentityFilter.class)
        .addFilterAfter(context.getBean(AppIdentityFilter.class), LegacyAuthorizationFilter.class)
        .addFilterAfter(context.getBean(JwtRequestFilter.class), AppIdentityFilter.class)
        .addFilterAfter(
            context.getBean(AuthPreCheckCreationRequestFilter.class), JwtRequestFilter.class)
        .addFilterAfter(
            context.getBean(EditorAuthorizationFilter.class),
            AuthPreCheckCreationRequestFilter.class)
        .addFilterAfter(
            context.getBean(GrSciCollEditorAuthorizationFilter.class),
            EditorAuthorizationFilter.class)
        .addFilterAfter(
            context.getBean(ResourceNotFoundRequestFilter.class),
            GrSciCollEditorAuthorizationFilter.class)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(corsAllOriginsAndMethodsConfiguration()))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new RegistryPasswordEncoder();
  }
}
