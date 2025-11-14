package com.signuplogin.demo.repository;


import com.signuplogin.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserId(int userId);
    List<Order> findByUserIdAndStatus(int userId, String status);

}
