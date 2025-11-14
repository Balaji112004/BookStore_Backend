package com.signuplogin.demo.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")  // ✅ "order" is reserved word in SQL
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // ✅ Each order belongs to one user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Signup user;

    private double totalAmount;

    private String status;  // e.g. "PENDING", "COMPLETED", "CANCELLED"

    private LocalDateTime orderDate = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<OrderItem> items;


    // ---- Getters & Setters ----
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Signup getUser() {
        return user;
    }
    public void setUser(Signup user) {
        this.user = user;
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
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    public List<OrderItem> getItems() {
        return items;
    }
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}

