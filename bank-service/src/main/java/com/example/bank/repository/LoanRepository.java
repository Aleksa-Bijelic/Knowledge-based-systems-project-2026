package com.example.bank.repository;

import com.example.bank.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByClientId(Long clientId);
    List<Loan> findByClientIdAndStatus(Long clientId, String status);
    List<Loan> findByStatus(String status);
}
