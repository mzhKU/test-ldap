package com.example.test_ldap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        http
              .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
              )
              .authenticationProvider(authenticationProvider)
              .httpBasic(Customizer.withDefaults())
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
