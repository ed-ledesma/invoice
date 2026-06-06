package com.invoice.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DtoCartItem {

    @JsonProperty("cartItemId")
    private Integer cartItemId;

    @JsonProperty("gtin")
    private String gtin;

    @JsonProperty("quantity")
    private Integer quantity;

    public Integer getCartItemId() { return cartItemId; }
    public void setCartItemId(Integer cartItemId) { this.cartItemId = cartItemId; }
    public String getGtin() { return gtin; }
    public void setGtin(String gtin) { this.gtin = gtin; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}