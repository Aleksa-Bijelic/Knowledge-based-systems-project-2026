package com.example.bank.controller;

import com.example.bank.dto.PackageAccountRequest;
import com.example.bank.dto.PackageAccountResponse;
import com.example.bank.service.PackageAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/package-accounts")
public class PackageAccountController {

    private final PackageAccountService packageAccountService;

    public PackageAccountController(PackageAccountService packageAccountService) {
        this.packageAccountService = packageAccountService;
    }

    @PostMapping
    public ResponseEntity<?> createPackageAccount(@RequestBody PackageAccountRequest request) {
        String username = currentUsername();
        try {
            PackageAccountResponse response = packageAccountService.createPackageAccount(username, request);
            return ResponseEntity.created(URI.create("/api/package-accounts/" + response.getId())).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping
    public List<PackageAccountResponse> getMyPackageAccounts() {
        return packageAccountService.getPackageAccountsForClient(currentUsername());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPackageAccount(@PathVariable("id") Long id) {
        try {
            PackageAccountResponse response = packageAccountService.getPackageAccountForClient(currentUsername(), id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
