package com.example.bookstore.config;

import com.example.bookstore.model.AppUser;
import com.example.bookstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminUsername = "admin";
            if (!userRepository.existsByUsername(adminUsername)) {
                AppUser admin = new AppUser();
                admin.setUsername(adminUsername);
                admin.setEmail("admin@example.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ROLE_ADMIN");
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);
                System.out.println("Created default admin user: admin / admin123");
            }
        };
    }
}
