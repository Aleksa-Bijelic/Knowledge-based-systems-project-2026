package com.example.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerUsername;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(name = "discount_amount", nullable = false, columnDefinition = "double precision default 0")
    private double discountAmount = 0.0;

    @Column(name = "final_amount", nullable = false, columnDefinition = "double precision default 0")
    private double finalAmount = 0.0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.discountAmount = 0.0;
        this.finalAmount = 0.0;
    }

    public Order(String customerUsername, LocalDateTime createdAt, double totalAmount, String status, String paymentMethod) {
        this.customerUsername = customerUsername;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.discountAmount = 0.0;
        this.finalAmount = totalAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        for (OrderItem item : items) {
            item.setOrder(this);
        }
    }

    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }
}
