package com.invoice.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DtoProduct {

    @JsonProperty("gtin")
    private String gtin;

    @JsonProperty("product")
    private String product;

    @JsonProperty("price")
    private Float price;

    @JsonProperty("stock")
    private Integer stock;

    public String getGtin() { return gtin; }
    public void setGtin(String gtin) { this.gtin = gtin; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}