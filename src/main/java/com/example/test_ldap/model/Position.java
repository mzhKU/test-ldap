package com.example.test_ldap.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Position within a portfolio")
public class Position {

    @Schema(description = "Unique identifier of the position", example = "1")
    private Long id;

    @Schema(description = "Portfolio ID this position belongs to", example = "1")
    private Long portfolioId;

    @Schema(description = "Symbol or ticker of the asset", example = "AAPL")
    private String symbol;

    @Schema(description = "Quantity of the asset held", example = "100")
    private Double quantity;

    @Schema(description = "Purchase price per unit", example = "150.50")
    private Double purchasePrice;

    @Schema(description = "Current market price per unit", example = "175.25")
    private Double currentPrice;

    public Position() {
    }

    public Position(Long id, Long portfolioId, String symbol, Double quantity, Double purchasePrice, Double currentPrice) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.currentPrice = currentPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }
}
