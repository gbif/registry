package org.gbif.registry.ws.config;

import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.ws.security.IdentityFilter;
import org.gbif.registry.ws.security.jwt.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

  private final ApplicationContext context;

  private UserDetailsService userDetailsService;

  public WebSecurityConfigurer(@Qualifier("registryUserDetailsService") UserDetailsService userDetailsService, ApplicationContext context) {
    this.userDetailsService = userDetailsService;
    this.context = context;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(dbAuthenticationProvider());
  }

  private DaoAuthenticationProvider dbAuthenticationProvider() {
    final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
        .httpBasic().and()
        // must be after this otherwise it would load the context from the previous call
        .addFilterAfter(context.getBean(IdentityFilter.class), SecurityContextPersistenceFilter.class)
        .addFilterAfter(context.getBean(JwtRequestFilter.class), IdentityFilter.class)
        .csrf().disable()
        .authorizeRequests()
        .anyRequest().authenticated();

    http
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new RegistryPasswordEncoder();
  }

}
