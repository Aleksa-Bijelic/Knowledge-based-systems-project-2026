package com.example.bank.config;

import com.example.bank.model.BankUser;
import com.example.bank.repository.BankUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDefaultUsers(BankUserRepository bankUserRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String officerUsername = "sluzbenik";
            if (!bankUserRepository.existsByUsername(officerUsername)) {
                BankUser officer = new BankUser();
                officer.setUsername(officerUsername);
                officer.setEmail("sluzbenik@bank.example.com");
                officer.setPassword(passwordEncoder.encode("sluzbenik123"));
                officer.setFirstName("Petar");
                officer.setLastName("Petrović");
                officer.setRole("ROLE_OFFICER");
                officer.setCreatedAt(LocalDateTime.now());
                bankUserRepository.save(officer);
                System.out.println("Created default bank officer: sluzbenik / sluzbenik123");
            }

            String clientUsername = "oliva";
            if (!bankUserRepository.existsByUsername(clientUsername)) {
                BankUser client = new BankUser();
                client.setUsername(clientUsername);
                client.setEmail("oliva@bank.example.com");
                client.setPassword(passwordEncoder.encode("oliva123"));
                client.setFirstName("Oliva");
                client.setLastName("Maslina");
                client.setRole("ROLE_CLIENT");
                client.setCreatedAt(LocalDateTime.now());
                bankUserRepository.save(client);
                System.out.println("Created default bank client: oliva / oliva123");
            }
        };
    }
}
