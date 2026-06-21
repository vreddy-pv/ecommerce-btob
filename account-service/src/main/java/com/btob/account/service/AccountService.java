package com.btob.account.service;

import com.btob.account.dto.AccountDto;
import com.btob.account.dto.AuthResponse;
import com.btob.account.entity.Account;
import com.btob.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account management service.
 * Satisfies ACCT-01 (credit limits, balance) and ACCT-02 (tier determines pricing).
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final ApiKeyService apiKeyService;

    /**
     * Authenticate user and return JWT token.
     * Satisfies AUTH-01 (login returns JWT).
     */
    public AuthResponse login(String email, String password) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtTokenService.generateToken(account);

        return AuthResponse.builder()
                .token(token)
                .accountId(account.getId())
                .email(account.getEmail())
                .tier(account.getTier())
                .expiresIn(86400000L) // 24 hours
                .build();
    }

    /**
     * Create a new account with hashed password and API key.
     */
    public AuthResponse createAccount(AccountDto dto) {
        if (accountRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Account account = Account.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .companyName(dto.getCompanyName())
                .tier(dto.getTier())
                .creditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO)
                .build();

        account = accountRepository.save(account);
        apiKeyService.generateApiKey(account);

        String token = jwtTokenService.generateToken(account);

        return AuthResponse.builder()
                .token(token)
                .accountId(account.getId())
                .email(account.getEmail())
                .tier(account.getTier())
                .expiresIn(86400000L)
                .build();
    }

    /**
     * Get account by ID.
     */
    public Account getAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    /**
     * Get account by email.
     */
    public Account getAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    /**
     * Get all accounts.
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Update credit limit.
     * Satisfies ACCT-01 (credit limits, balance).
     */
    public Account updateCreditLimit(UUID id, BigDecimal newLimit) {
        Account account = getAccount(id);
        account.setCreditLimit(newLimit);
        return accountRepository.save(account);
    }

    /**
     * Update current balance.
     * Satisfies ACCT-01 (credit limits, balance).
     */
    public Account updateBalance(UUID id, BigDecimal amount) {
        Account account = getAccount(id);
        account.setCurrentBalance(amount);
        return accountRepository.save(account);
    }
}