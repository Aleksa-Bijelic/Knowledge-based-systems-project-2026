package com.example.bank.controller;

import com.example.bank.dto.LoanRequestDTO;
import com.example.bank.dto.LoanRequestHistoryDTO;
import com.example.bank.dto.LoanResponseDTO;
import com.example.bank.model.BankUser;
import com.example.bank.model.LoanRequest;
import com.example.bank.repository.BankUserRepository;
import com.example.bank.repository.LoanRequestRepository;
import com.example.bank.rules.LoanAssessment;
import com.example.bank.rules.LoanRequestFact;
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
    private final LoanRequestRepository loanRequestRepository;

    public LoanController(LoanService loanService, BankUserRepository bankUserRepository,
                          LoanRequestRepository loanRequestRepository) {
        this.loanService = loanService;
        this.bankUserRepository = bankUserRepository;
        this.loanRequestRepository = loanRequestRepository;
    }

    @PostMapping("/assess")
    public ResponseEntity<LoanResponseDTO> assessLoan(@RequestBody LoanRequestDTO request) {
        LoanRequestFact loanRequest = new LoanRequestFact(
                request.getClientId(),
                request.getLoanAmount(),
                request.getNumberOfInstallments(),
                request.getEmploymentStatus(),
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        LoanAssessment assessment = loanService.evaluateLoanRequest(loanRequest);

        LoanRequest saved = loanService.saveLoanRequest(
                request.getClientId(),
                request.getLoanAmount(),
                request.getNumberOfInstallments(),
                request.getEmploymentStatus(),
                request.getContractStartDate(),
                request.getContractEndDate(),
                assessment
        );

        LoanResponseDTO response = new LoanResponseDTO(
                saved.getId(),
                loanRequest.getClientId(),
                assessment.isApproved(),
                assessment.getReasons(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getMonthlyPayment(),
                assessment.getDebtToIncomeRatio()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/decision")
    public ResponseEntity<Map<String, Object>> recordDecision(@RequestBody Map<String, Object> body) {
        Long requestId = Long.valueOf(body.get("requestId").toString());
        String officerDecision = (String) body.get("officerDecision");
        String officerUsername = (String) body.getOrDefault("officerUsername", "unknown");

        LoanRequest updated = loanService.saveOfficerDecision(
                requestId, officerDecision, officerUsername);

        Map<String, Object> response = new HashMap<>();
        response.put("requestId", updated.getId());
        response.put("clientId", updated.getClientId());
        response.put("officerDecision", updated.getOfficerDecision());
        response.put("status", "RECORDED");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<LoanRequestHistoryDTO>> getLoanHistory() {
        List<LoanRequest> requests = loanRequestRepository.findAll();
        List<LoanRequestHistoryDTO> history = requests.stream().map(req -> {
            String clientName = bankUserRepository.findById(req.getClientId())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Unknown");
            return new LoanRequestHistoryDTO(
                    req.getId(),
                    req.getClientId(),
                    clientName,
                    req.getLoanAmount(),
                    req.getNumberOfInstallments(),
                    req.getEmploymentStatus(),
                    req.getStatus(),
                    req.getRiskScore(),
                    req.getRiskLevel(),
                    req.getMonthlyPayment(),
                    req.getDebtToIncomeRatio(),
                    req.getSystemRecommendation(),
                    req.getOfficerDecision(),
                    req.getOfficerUsername(),
                    req.getCreatedAt()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(history);
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
