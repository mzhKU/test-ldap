
## Dependency Breakdown

1. unboundid-ldapsdk
Purpose: Provides the embedded LDAP server implementation

What it adds:

InMemoryDirectoryServer - The actual LDAP server that runs in-memory
InMemoryDirectoryServerConfig - Configuration for the server
LDIF parsing and loading capabilities
LDAP protocol implementation
Used for: Running a test/embedded LDAP server without needing an external LDAP installation. Perfect for development and testing.

In your project: Spring Boot's autoconfiguration uses this to create and start the embedded LDAP server on port 8389 with your test-server.ldif data.

2. spring-boot-starter-data-ldap
Purpose: Spring Boot autoconfiguration for LDAP connectivity

What it adds:

LdapContextSource - Connection pool and context management for LDAP
LdapTemplate - Spring's template for LDAP operations (like JdbcTemplate)
Spring Data LDAP repositories (if you want to use them)
Autoconfiguration for embedded LDAP server (uses unboundid-ldapsdk)
Autoconfiguration for LDAP client connection
Used for:

Connecting to LDAP servers (embedded or external)
Providing the infrastructure beans that Spring Security LDAP needs
Automatic setup based on application.yaml properties
In your project: Creates the ldapContextSource bean that your SecurityConfig uses for authentication.

3. spring-security-ldap
Purpose: Spring Security integration with LDAP for authentication/authorization

What it adds:

LdapAuthenticationProvider - Authenticates users against LDAP
BindAuthenticator - Performs LDAP bind operations to verify passwords
FilterBasedLdapUserSearch - Searches for users in LDAP directory
DefaultLdapAuthoritiesPopulator - Retrieves user roles/groups from LDAP
LdapUserDetailsMapper - Maps LDAP entries to Spring Security's UserDetails
Used for: The actual authentication logic - verifying credentials and loading user authorities from LDAP.

In your project: You manually configured these components in SecurityConfig.ldapAuthenticationProvider()
to define how authentication works.

Dependency Relationship
┌─────────────────────────────────────┐
│  spring-boot-starter-data-ldap      │
│  (Autoconfiguration & Connection)   │
│                                     │
│   ┌──────────────────────────────┐  │
│   │  unboundid-ldapsdk           │  │
│   │  (Embedded LDAP Server)      │  │
│   └──────────────────────────────┘  │
└─────────────────────────────────────┘
                     ↓
           Provides contextSource
                     ↓
┌─────────────────────────────────────┐
│  spring-security-ldap               │
│  (Authentication Logic)             │
│                                     │
│  Uses contextSource to:             │
│  - Search for users                 │
│  - Bind/authenticate                │
│  - Load authorities                 │
└─────────────────────────────────────┘

Could You Remove Any?

Remove unboundid-ldapsdk?               Yes, if connecting to external LDAP server (not embedded)
Remove spring-boot-starter-data-ldap?   No, you need the LdapContextSource for Spring Security LDAP
Remove spring-security-ldap?            No, this is what actually does the authentication
For production with an external LDAP server, you'd keep spring-boot-starter-data-ldap and spring-security-ldap,
but remove unboundid-ldapsdk.


## SEQUENCE DIAGRAM

┌─────────┐          ┌──────────────┐          ┌─────────────────┐          ┌────────────┐          ┌─────────────┐
│ Client  │          │ Spring       │          │ LDAP            │          │ LDAP       │          │ Controller  │
│ (curl)  │          │ Security     │          │ Authentication  │          │ Server     │          │ Method      │
└────┬────┘          └──────┬───────┘          │ Provider        │          └─────┬──────┘          └──────┬──────┘
     │                      │                  └────────┬────────┘                │                        │
     │ 1. HTTP Request      │                           │                         │                        │
     │ with Basic Auth      │                           │                         │                        │
     │ admin:admin123       │                           │                         │                        │
     ├─────────────────────>│                           │                         │                        │
     │                      │                           │                         │                        │
     │                      │ 2. Extract credentials    │                         │                        │
     │                      │    from Authorization     │                         │                        │
     │                      │    header                 │                         │                        │
     │                      │                           │                         │                        │
     │                      │ 3. authenticate(username, │                         │                        │
     │                      │    password)              │                         │                        │
     │                      ├──────────────────────────>│                         │                        │
     │                      │                           │                         │                        │
     │                      │                           │ 4. Search for user      │                        │
     │                      │                           │    in ou=people         │                        │
     │                      │                           │    filter: (uid=admin)  │                        │
     │                      │                           ├────────────────────────>│                        │
     │                      │                           │                         │                        │
     │                      │                           │ 5. Return user DN       │                        │
     │                      │                           │    uid=admin,ou=people, │                        │
     │                      │                           │    dc=example,dc=com    │                        │
     │                      │                           │<────────────────────────┤                        │
     │                      │                           │                         │                        │
     │                      │                           │ 6. Bind (authenticate)  │                        │
     │                      │                           │    with user DN and     │                        │
     │                      │                           │    password             │                        │
     │                      │                           ├────────────────────────>│                        │
     │                      │                           │                         │                        │
     │                      │                           │ 7. Bind success         │                        │
     │                      │                           │<────────────────────────┤                        │
     │                      │                           │                         │                        │
     │                      │                           │ 8. Search for roles     │                        │
     │                      │                           │    in ou=groups         │                        │
     │                      │                           │    filter: (member=     │                        │
     │                      │                           │    uid=admin,ou=people, │                        │
     │                      │                           │    dc=example,dc=com)   │                        │
     │                      │                           ├────────────────────────>│                        │
     │                      │                           │                         │                        │
     │                      │                           │ 9. Return groups:       │                        │
     │                      │                           │    ROLE_ADMIN,          │                        │
     │                      │                           │    ROLE_USER            │                        │
     │                      │                           │<────────────────────────┤                        │
     │                      │                           │                         │                        │
     │                      │ 10. Return Authentication │                         │                        │
     │                      │     with authorities      │                         │                        │
     │                      │<──────────────────────────┤                         │                        │
     │                      │                           │                         │                        │
     │                      │ 11. Store in              │                         │                        │
     │                      │     SecurityContext       │                         │                        │
     │                      │                           │                         │                        │
     │                      │ 12. Check @PreAuthorize   │                         │                        │
     │                      │     hasAuthority          │                         │                        │
     │                      │     ('ROLE_ADMIN')        │                         │                        │
     │                      │                           │                         │                        │
     │                      │ 13. Authorization OK      │                         │                        │
     │                      ├─────────────────────────────────────────────────────────────────────────────>│
     │                      │                           │                         │                        │
     │                      │                           │                         │                        │ 14. Execute
     │                      │                           │                         │                        │     method
     │                      │                           │                         │                        │
     │                      │ 15. Return response       │                         │                        │
     │<─────────────────────┼──────────────────────────────────────────────────────────────────────────────┤
     │ 200 OK / 201 Created │                           │                         │                        │
     │                      │                           │                         │                        │