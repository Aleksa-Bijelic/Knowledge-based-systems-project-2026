package com.example.bank.repository;

import com.example.bank.model.PackageAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageAccountRepository extends JpaRepository<PackageAccount, Long> {
    List<PackageAccount> findByClientId(Long clientId);
    List<PackageAccount> findByClientUsername(String username);
    boolean existsByNameAndClientUsername(String name, String username);
}
