package com.btob.account.controller;

import com.btob.account.dto.AccountDto;
import com.btob.account.dto.AuthResponse;
import com.btob.account.dto.LoginRequest;
import com.btob.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for login and registration.
 * Satisfies AUTH-01 (login returns JWT).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;

    /**
     * Login with email and password.
     * Returns JWT token for successful authentication.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = accountService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    /**
     * Register a new account.
     * Returns JWT token for immediate login.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AccountDto request) {
        AuthResponse response = accountService.createAccount(request);
        return ResponseEntity.ok(response);
    }
}