package com.example.bank.controller;

import com.example.bank.dto.LoanRequestDTO;
import com.example.bank.dto.LoanResponseDTO;
import com.example.bank.model.BankUser;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.rules.LoanAssessment;
import com.example.bank.rules.LoanRequest;
import com.example.bank.rules.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final BankUserRepository bankUserRepository;

    public LoanController(LoanService loanService, BankUserRepository bankUserRepository) {
        this.loanService = loanService;
        this.bankUserRepository = bankUserRepository;
    }

    @PostMapping("/assess")
    public ResponseEntity<LoanResponseDTO> assessLoan(@RequestBody LoanRequestDTO request) {
        LoanRequest loanRequest = new LoanRequest(
                request.getClientId(),
                request.getLoanAmount(),
                request.getNumberOfInstallments(),
                request.getEmploymentStatus(),
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        LoanAssessment assessment = loanService.evaluateLoanRequest(loanRequest);

        LoanResponseDTO response = new LoanResponseDTO(
                assessment.isApproved(),
                assessment.getReasons(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getMonthlyPayment(),
                assessment.getDebtToIncomeRatio()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<Map<String, Object>>> getClients() {
        List<BankUser> clients = bankUserRepository.findByRole("ROLE_CLIENT");
        List<Map<String, Object>> clientList = clients.stream().map(client -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", client.getId());
            map.put("username", client.getUsername());
            map.put("firstName", client.getFirstName());
            map.put("lastName", client.getLastName());
            map.put("email", client.getEmail());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(clientList);
    }
}
