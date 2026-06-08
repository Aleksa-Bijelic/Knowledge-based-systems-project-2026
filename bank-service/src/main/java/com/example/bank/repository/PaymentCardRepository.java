package com.example.bank.repository;

import com.example.bank.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    Optional<PaymentCard> findByCardNumber(String cardNumber);
    boolean existsByCardNumber(String cardNumber);
}
