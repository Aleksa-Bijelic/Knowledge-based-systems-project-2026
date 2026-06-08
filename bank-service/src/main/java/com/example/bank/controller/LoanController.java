package com.example.bank.controller;

import com.example.bank.dto.LoanRequestDTO;
import com.example.bank.dto.LoanResponseDTO;
import com.example.bank.rules.LoanAssessment;
import com.example.bank.rules.LoanRequest;
import com.example.bank.rules.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
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
}
