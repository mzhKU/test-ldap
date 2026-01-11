package com.example.test_ldap.controller;

import com.example.test_ldap.model.Portfolio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio", description = "Portfolio management APIs")
public class PortfolioController {

    private final ConcurrentHashMap<Long, Portfolio> portfolios = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Operation(summary = "Get all portfolios", description = "Retrieve a list of all portfolios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Portfolio>> getAllPortfolios(Authentication authentication) {

        // Note: @PreAuthorized("isAuthenticated()") is needed, otherwise authentication
        //       parameter would be null

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) {
            // Admin sees all portfolios
            return ResponseEntity.ok(new ArrayList<>(portfolios.values()));
        } else {
            // Regular users only see their own portfolios
            String ldapUserName = authentication.getName();
            var result = portfolios.values().stream()
                    .filter(p -> p.getLdapUserName().equals(ldapUserName))
                    .toList();
            return ResponseEntity.ok(result);
        }
    }

    @Operation(summary = "Get portfolio by ID", description = "Retrieve a specific portfolio by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved portfolio",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable Long id, Authentication auth) {
        Portfolio portfolio = portfolios.get(id);
        if (portfolio == null) {
            return ResponseEntity.notFound().build();
        }
        
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        // Admin can access any portfolio, regular users only their own
        if (!isAdmin && !portfolio.getLdapUserName().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(portfolio);
    }

    @Operation(summary = "Create a new portfolio", description = "Create a new portfolio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Portfolio created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody Portfolio portfolio, Authentication auth) {
        Long id = idCounter.getAndIncrement();
        portfolio.setId(id);
        portfolio.setLdapUserName(auth.getName());
        portfolios.put(id, portfolio);
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
    }

    @Operation(summary = "Update a portfolio", description = "Update an existing portfolio by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Portfolio> updatePortfolio(
            @Parameter(description = "ID of the portfolio to update") @PathVariable Long id,
            @RequestBody Portfolio portfolio) {
        if (!portfolios.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        portfolio.setId(id);
        portfolios.put(id, portfolio);
        return ResponseEntity.ok(portfolio);
    }

    @Operation(summary = "Delete a portfolio", description = "Delete a portfolio by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Portfolio deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePortfolio(
            @Parameter(description = "ID of the portfolio to delete") @PathVariable Long id) {
        if (!portfolios.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        portfolios.remove(id);
        return ResponseEntity.noContent().build();
    }
}
