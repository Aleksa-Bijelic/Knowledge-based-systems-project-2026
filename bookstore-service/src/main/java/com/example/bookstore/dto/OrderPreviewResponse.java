package com.example.bookstore.dto;

import java.util.List;

public class OrderPreviewResponse {
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String selectedDiscountType;
    private List<OrderItemDiscountResponse> items;

    public OrderPreviewResponse() {
    }

    public OrderPreviewResponse(double totalAmount, double discountAmount, double finalAmount,
                                String selectedDiscountType, List<OrderItemDiscountResponse> items) {
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.selectedDiscountType = selectedDiscountType;
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getSelectedDiscountType() {
        return selectedDiscountType;
    }

    public void setSelectedDiscountType(String selectedDiscountType) {
        this.selectedDiscountType = selectedDiscountType;
    }

    public List<OrderItemDiscountResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDiscountResponse> items) {
        this.items = items;
    }
}
