package com.example.bookstore.rules;

import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;

import java.util.HashMap;
import java.util.Map;

public class DiscountContext {

    private final Order order;
    private Map<OrderItem, Double> itemDiscounts = new HashMap<>();
    private double itemDiscountTotal;
    private double orderDiscountTotal;
    private double chosenDiscount;
    private String selectedDiscountType;

    public DiscountContext(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public Map<OrderItem, Double> getItemDiscounts() {
        return itemDiscounts;
    }

    public void setItemDiscounts(Map<OrderItem, Double> itemDiscounts) {
        this.itemDiscounts = itemDiscounts;
    }

    public double getItemDiscount(OrderItem item) {
        return itemDiscounts.getOrDefault(item, 0.0);
    }

    public double getItemDiscountTotal() {
        return itemDiscountTotal;
    }

    public void setItemDiscountTotal(double itemDiscountTotal) {
        this.itemDiscountTotal = itemDiscountTotal;
    }

    public double getOrderDiscountTotal() {
        return orderDiscountTotal;
    }

    public void setOrderDiscountTotal(double orderDiscountTotal) {
        this.orderDiscountTotal = Math.max(this.orderDiscountTotal, orderDiscountTotal);
    }

    public double getChosenDiscount() {
        return chosenDiscount;
    }

    public void setChosenDiscount(double chosenDiscount) {
        this.chosenDiscount = chosenDiscount;
    }

    public String getSelectedDiscountType() {
        return selectedDiscountType;
    }

    public void setSelectedDiscountType(String selectedDiscountType) {
        this.selectedDiscountType = selectedDiscountType;
    }
}
