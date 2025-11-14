package com.signuplogin.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PracticeApplication {


    public static void main(String[] args) {
        SpringApplication.run(PracticeApplication.class, args);

        // Correct way: get the driver via DataSource
        // Or use ApplicationContext to get @Value
    }
}

