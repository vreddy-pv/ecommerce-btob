package com.btob.account.controller;

import com.btob.account.entity.Account;
import com.btob.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Account management controller.
 * Satisfies ACCT-01 (credit limits, balance) and ACCT-02 (tier determines pricing).
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Get account by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable UUID id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }

    /**
     * Get all accounts.
     */
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Update credit limit.
     * Satisfies ACCT-01 (credit limits, balance).
     */
    @PutMapping("/{id}/credit-limit")
    public ResponseEntity<Account> updateCreditLimit(
            @PathVariable UUID id,
            @RequestBody BigDecimal newLimit) {
        Account account = accountService.updateCreditLimit(id, newLimit);
        return ResponseEntity.ok(account);
    }

    /**
     * Update current balance.
     * Satisfies ACCT-01 (credit limits, balance).
     */
    @PutMapping("/{id}/balance")
    public ResponseEntity<Account> updateBalance(
            @PathVariable UUID id,
            @RequestBody BigDecimal amount) {
        Account account = accountService.updateBalance(id, amount);
        return ResponseEntity.ok(account);
    }
}