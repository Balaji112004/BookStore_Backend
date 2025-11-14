package com.signuplogin.demo.entity;

// CartResponse.java

public class CartResponse {
    private int productId;
    private String title;
    private double newPrice;
    private int quantity;
    private String coverImage;

    // Constructor
    public CartResponse(int productId, String title, double newPrice, int quantity, String coverImage) {
        this.productId = productId;
        this.title = title;
        this.newPrice = newPrice;
        this.quantity = quantity;
        this.coverImage = coverImage;
    }

    // ✅ Getters & Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getNewPrice() { return newPrice; }
    public void setNewPrice(double newPrice) { this.newPrice = newPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
}

