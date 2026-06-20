package com.example.bank.repository;

import com.example.bank.model.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
    List<LoanRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<LoanRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<LoanRequest> findByOfficerUsernameOrderByCreatedAtDesc(String officerUsername);
}
