package com.example.bank.repository;

import com.example.bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderAccountNumber(String accountNumber);
    List<Transaction> findByReceiverAccountNumber(String accountNumber);
    List<Transaction> findBySenderAccountNumberOrReceiverAccountNumber(String senderAccountNumber, String receiverAccountNumber);
    List<Transaction> findByStatus(String status);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountNumber = :account AND t.status = 'COMPLETED' ORDER BY t.createdAt DESC")
    List<Transaction> findRecentCompletedBySenderAccount(@Param("account") String account);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountNumber IN :accounts AND t.status IN ('COMPLETED', 'APPROVED') ORDER BY t.createdAt DESC")
    List<Transaction> findCompletedBySenderAccounts(@Param("accounts") List<String> accounts);

    @Query("SELECT t FROM Transaction t WHERE t.status IN ('COMPLETED', 'APPROVED') ORDER BY t.createdAt ASC")
    List<Transaction> findAllCompletedOrApproved();

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountNumber IN " +
           "(SELECT ba.accountNumber FROM BankAccount ba WHERE ba.packageAccount.client.id = :clientId) " +
           "AND t.status = 'SUSPICIOUS'")
    List<Transaction> findSuspiciousByClientId(@Param("clientId") Long clientId);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountNumber IN " +
           "(SELECT ba.accountNumber FROM BankAccount ba WHERE ba.packageAccount.client.id = :clientId) " +
           "AND t.id = :transactionId")
    List<Transaction> findByIdAndClientId(@Param("transactionId") Long transactionId, @Param("clientId") Long clientId);
}
