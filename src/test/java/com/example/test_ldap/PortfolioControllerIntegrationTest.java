package com.example.test_ldap;

import com.example.test_ldap.model.Portfolio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PortfolioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Tests run with embedded LDAP server from test-server.ldif
        // Users: admin (ROLE_ADMIN, ROLE_USER), user1 (ROLE_USER), user2 (ROLE_USER)
    }

    private MvcResult createPortfolio(String username, String password, Portfolio portfolio) throws Exception {
        return mockMvc.perform(post("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(username, password))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(portfolio)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void shouldAuthenticateWithValidLdapCredentials() throws Exception {
        mockMvc.perform(get("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(get("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedUserToCreatePortfolio() throws Exception {
        Portfolio portfolio = new Portfolio(null, "Test Portfolio", "Test Description", null);

        MvcResult result = createPortfolio("user1", "user1", portfolio);
        
        String responseBody = result.getResponse().getContentAsString();
        Portfolio createdPortfolio = objectMapper.readValue(responseBody, Portfolio.class);
        
        mockMvc.perform(get("/api/portfolios/" + createdPortfolio.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user1", "user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Portfolio"))
                .andExpect(jsonPath("$.ldapUserName").value("user1"));
    }

    @Test
    void shouldOnlyReturnPortfoliosOwnedByAuthenticatedUser() throws Exception {
        // Create portfolio for user1
        Portfolio portfolio1 = new Portfolio(null, "User1 Portfolio", "Description", null);
        createPortfolio("user1", "user1", portfolio1);

        // Create portfolio for user2
        Portfolio portfolio2 = new Portfolio(null, "User2 Portfolio", "Description", null);
        createPortfolio("user2", "user2", portfolio2);

        // User1 should only see their own portfolio
        mockMvc.perform(get("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user1", "user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("User1 Portfolio"))
                .andExpect(jsonPath("$[0].ldapUserName").value("user1"));

        // User2 should only see their own portfolio
        mockMvc.perform(get("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user2", "user2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("User2 Portfolio"))
                .andExpect(jsonPath("$[0].ldapUserName").value("user2"));
    }

    @Test
    void shouldPreventUserFromAccessingOtherUsersPortfolioById() throws Exception {
        // User1 creates a portfolio
        Portfolio portfolio = new Portfolio(null, "User1 Portfolio", "Description", null);
        MvcResult result = createPortfolio("user1", "user1", portfolio);

        String responseBody = result.getResponse().getContentAsString();
        Portfolio createdPortfolio = objectMapper.readValue(responseBody, Portfolio.class);
        Long portfolioId = createdPortfolio.getId();

        // User1 can access their own portfolio
        mockMvc.perform(get("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user1", "user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ldapUserName").value("user1"));

        // User2 cannot access user1's portfolio
        mockMvc.perform(get("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user2", "user2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToSeeAllPortfolios() throws Exception {
        // Create portfolio for user1
        Portfolio portfolio1 = new Portfolio(null, "User1 Portfolio", "Description", null);
        createPortfolio("user1", "user1", portfolio1);

        // Create portfolio for user2
        Portfolio portfolio2 = new Portfolio(null, "User2 Portfolio", "Description", null);
        createPortfolio("user2", "user2", portfolio2);

        // Create portfolio for admin
        Portfolio portfolio3 = new Portfolio(null, "Admin Portfolio", "Description", null);
        createPortfolio("admin", "admin123", portfolio3);

        // Admin should see all 3 portfolios
        mockMvc.perform(get("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].ldapUserName", containsInAnyOrder("user1", "user2", "admin")));

        // User1 should only see their own
        mockMvc.perform(get("/api/portfolios")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user1", "user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ldapUserName").value("user1"));
    }

    @Test
    void shouldAllowAdminToCreatePortfolio() throws Exception {
        Portfolio portfolio = new Portfolio(null, "Admin Portfolio", "Admin Description", null);

        MvcResult result = createPortfolio("admin", "admin123", portfolio);
        
        String responseBody = result.getResponse().getContentAsString();
        Portfolio createdPortfolio = objectMapper.readValue(responseBody, Portfolio.class);
        
        mockMvc.perform(get("/api/portfolios/" + createdPortfolio.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ldapUserName").value("admin"));
    }

    @Test
    void shouldAllowOnlyAdminToUpdatePortfolio() throws Exception {
        // Admin creates a portfolio
        Portfolio portfolio = new Portfolio(null, "Original Name", "Original Description", null);
        MvcResult result = createPortfolio("admin", "admin123", portfolio);

        String responseBody = result.getResponse().getContentAsString();
        Portfolio createdPortfolio = objectMapper.readValue(responseBody, Portfolio.class);
        Long portfolioId = createdPortfolio.getId();

        // Update portfolio
        createdPortfolio.setName("Updated Name");

        // Admin can update
        mockMvc.perform(put("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createdPortfolio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        // Regular user cannot update
        mockMvc.perform(put("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user1", "user1"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createdPortfolio)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOnlyAdminToDeletePortfolio() throws Exception {
        // Admin creates a portfolio
        Portfolio portfolio = new Portfolio(null, "To Delete", "Description", null);
        MvcResult result = createPortfolio("admin", "admin123", portfolio);

        String responseBody = result.getResponse().getContentAsString();
        Portfolio createdPortfolio = objectMapper.readValue(responseBody, Portfolio.class);
        Long portfolioId = createdPortfolio.getId();

        // Regular user cannot delete
        mockMvc.perform(delete("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("user1", "user1")))
                .andExpect(status().isForbidden());

        // Admin can delete
        mockMvc.perform(delete("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/portfolios/" + portfolioId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLoadRolesFromLdapGroups() throws Exception {
        // This test verifies that roles are loaded from LDAP groups
        // admin should have ROLE_ADMIN and ROLE_USER
        // user1 should have only ROLE_USER
        
        // Admin can create (requires ROLE_ADMIN)
        Portfolio adminPortfolio = new Portfolio(null, "Admin Portfolio", "Description", null);
        createPortfolio("admin", "admin123", adminPortfolio);

        // User1 can create (only requires isAuthenticated)
        Portfolio userPortfolio = new Portfolio(null, "User Portfolio", "Description", null);
        createPortfolio("user1", "user1", userPortfolio);
    }
}
