package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {

        // ✅ print bcrypt hash for "test123"
        System.out.println(new BCryptPasswordEncoder().encode("test123"));

        SpringApplication.run(DemoApplication.class, args);
    }
}

