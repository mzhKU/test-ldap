package com.example.test_ldap.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Note: The Authentication object is the same regardless of how the user authenticated:
    // - HTTP Basic Auth: "If the client sends HTTP Basic credentials, authenticate them"
    //   - Authentication object created from credentials
    //   - Looks for `Authorization: Basic ...` header in incoming request
    //   - If found, extracts and decodes credentials and authenticates
    // - Session-based:
    //   - Authentication object retrieved from session
    //   - Session policy is STATELESS → no session created, no JSESSIONID cookie sent back
    // - Form login → Authentication object created from form submission

    @Bean
    @Profile("basic")
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        http
              .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
              .authenticationProvider(authenticationProvider)
              .httpBasic(withDefaults())
              .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
              .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Profile("session")
    public SecurityFilterChain sessionAuthFilterChain(HttpSecurity http, AuthenticationProvider ldapAuthenticationProvider) throws Exception {
        http
              .authenticationProvider(ldapAuthenticationProvider)
              .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
              .httpBasic(Customizer.withDefaults())
              .formLogin(Customizer.withDefaults())
              .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
              )
              .sessionManagement(session -> session.sessionCreationPolicy(IF_REQUIRED))
              .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public AuthenticationProvider ldapAuthenticationProvider(BaseLdapPathContextSource contextSource) {
        // Authenticator validates username / password against LDAP
        System.out.println("Creating LDAP Authentication Provider");
        System.out.println("  Context Source Base: " + contextSource.getBaseLdapPathAsString());

        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
              "ou=people",
              "(uid={0})", // {0} is replaced with the username entered by the user
              contextSource
        );
        bindAuthenticator.setUserSearch(userSearch);

        // Retrieves roles from groups and converts group memberships into Spring Security authorities (roles)
        DefaultLdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
              contextSource,
              "ou=groups"
        );
        authoritiesPopulator.setGroupSearchFilter("(member={0})");
        authoritiesPopulator.setRolePrefix("");

        // The LdapAuthenticationProvider handles LDAP authentication.
        return new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);
    }

}
