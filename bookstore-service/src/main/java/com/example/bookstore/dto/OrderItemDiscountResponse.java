package com.example.bookstore.dto;

public class OrderItemDiscountResponse {
    private Long bookId;
    private String title;
    private int quantity;
    private double unitPrice;
    private double itemDiscount;

    public OrderItemDiscountResponse() {
    }

    public OrderItemDiscountResponse(Long bookId, String title, int quantity, double unitPrice, double itemDiscount) {
        this.bookId = bookId;
        this.title = title;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.itemDiscount = itemDiscount;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getItemDiscount() {
        return itemDiscount;
    }

    public void setItemDiscount(double itemDiscount) {
        this.itemDiscount = itemDiscount;
    }
}
