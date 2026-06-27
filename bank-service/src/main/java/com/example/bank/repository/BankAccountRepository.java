package com.example.bank.repository;

import com.example.bank.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    @Query("SELECT ba FROM BankAccount ba LEFT JOIN FETCH ba.packageAccount pa LEFT JOIN FETCH pa.client WHERE ba.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumberWithClient(@Param("accountNumber") String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    List<BankAccount> findByPackageAccountId(Long packageAccountId);

    @Query("SELECT ba.accountNumber FROM BankAccount ba WHERE ba.packageAccount.client.id = :clientId")
    List<String> findAccountNumbersByClientId(@Param("clientId") Long clientId);
}
