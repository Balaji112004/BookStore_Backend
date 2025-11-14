package com.signuplogin.demo.repository;

import com.signuplogin.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface productrepo extends JpaRepository<Product,Integer> {
    List<Product> findByTrending(boolean trending);
    List<Product> findByCategory(String category);
}
