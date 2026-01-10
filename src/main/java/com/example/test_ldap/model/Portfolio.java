package com.example.test_ldap.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Portfolio containing multiple positions")
public class Portfolio {

    @Schema(description = "Unique identifier of the portfolio", example = "1")
    private Long id;

    @Schema(description = "Name of the portfolio", example = "Tech Portfolio")
    private String name;

    @Schema(description = "Description of the portfolio", example = "Portfolio focused on technology stocks")
    private String description;

    @Schema(description = "List of positions in this portfolio")
    private List<Position> positions = new ArrayList<>();

    private String ldapUserName;

    public Portfolio() {
    }

    public Portfolio(Long id, String name, String description, String ldapUserName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ldapUserName = ldapUserName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    public String getLdapUserName() {
        return this.ldapUserName;
    }

    public void setLdapUserName(String ldapUserName) {
        this.ldapUserName = ldapUserName;
    }
}
