package com.example.bookstore.dto;

import java.util.List;

public class OrderRequest {
    private String customerUsername;
    private List<OrderItemRequest> items;

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
