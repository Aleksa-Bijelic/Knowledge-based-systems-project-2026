package com.example.bookstore.rules;

import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;

import java.util.HashMap;
import java.util.Map;

public class DiscountContext {

    private final Order order;
    private final Map<OrderItem, Double> itemDiscounts = new HashMap<>();
    private double orderDiscountTotal;
    private double chosenDiscount;
    private String selectedDiscountType;

    public DiscountContext(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public void addItemDiscount(OrderItem item, double discount) {
        if (item == null || discount <= 0) {
            return;
        }
        itemDiscounts.compute(item, (key, current) -> current == null ? discount : Math.max(current, discount));
    }

    public double getItemDiscountTotal() {
        return itemDiscounts.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getOrderDiscountTotal() {
        return orderDiscountTotal;
    }

    public void setOrderDiscountTotal(double orderDiscountTotal) {
        this.orderDiscountTotal = Math.max(this.orderDiscountTotal, orderDiscountTotal);
    }

    public double getItemDiscount(OrderItem item) {
        return itemDiscounts.getOrDefault(item, 0.0);
    }

    public Map<OrderItem, Double> getItemDiscounts() {
        return itemDiscounts;
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
