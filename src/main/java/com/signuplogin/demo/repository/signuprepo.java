package com.signuplogin.demo.repository;

import com.signuplogin.demo.entity.Signup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface signuprepo extends JpaRepository<Signup,Integer> {
    Signup findByNameAndPassword(String name,String password);
}
