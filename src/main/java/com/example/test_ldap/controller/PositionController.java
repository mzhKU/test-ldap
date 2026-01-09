package com.example.test_ldap.controller;

import com.example.test_ldap.model.Position;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/positions")
@Tag(name = "Position", description = "Position management APIs")
public class PositionController {

    private final ConcurrentHashMap<Long, Position> positions = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Operation(summary = "Get all positions", description = "Retrieve a list of all positions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Position>> getAllPositions(
            @Parameter(description = "Filter by portfolio ID") @RequestParam(required = false) Long portfolioId) {
        List<Position> result = new ArrayList<>(positions.values());
        if (portfolioId != null) {
            result = result.stream()
                    .filter(p -> portfolioId.equals(p.getPortfolioId()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get position by ID", description = "Retrieve a specific position by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved position",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "404", description = "Position not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Position> getPositionById(
            @Parameter(description = "ID of the position to retrieve") @PathVariable Long id) {
        Position position = positions.get(id);
        if (position == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(position);
    }

    @Operation(summary = "Create a new position", description = "Create a new position in a portfolio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Position created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Position> createPosition(@RequestBody Position position) {
        Long id = idCounter.getAndIncrement();
        position.setId(id);
        positions.put(id, position);
        return ResponseEntity.status(HttpStatus.CREATED).body(position);
    }

    @Operation(summary = "Update a position", description = "Update an existing position by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Position updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "404", description = "Position not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Position> updatePosition(
            @Parameter(description = "ID of the position to update") @PathVariable Long id,
            @RequestBody Position position) {
        if (!positions.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        position.setId(id);
        positions.put(id, position);
        return ResponseEntity.ok(position);
    }

    @Operation(summary = "Delete a position", description = "Delete a position by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Position deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Position not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePosition(
            @Parameter(description = "ID of the position to delete") @PathVariable Long id) {
        if (!positions.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        positions.remove(id);
        return ResponseEntity.noContent().build();
    }
}
