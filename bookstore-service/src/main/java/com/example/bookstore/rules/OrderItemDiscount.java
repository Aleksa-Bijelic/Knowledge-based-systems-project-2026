package com.example.bookstore.rules;

import com.example.bookstore.model.OrderItem;

public class OrderItemDiscount {

    private final OrderItem item;
    private final double discount;

    public OrderItemDiscount(OrderItem item, double discount) {
        this.item = item;
        this.discount = discount;
    }

    public OrderItem getItem() {
        return item;
    }

    public double getDiscount() {
        return discount;
    }
}
