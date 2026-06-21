package com.btob.account.service;

import com.btob.account.entity.Account;
import com.btob.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * API key validation and generation service.
 * Satisfies AUTH-03 (API keys for service-to-service authentication).
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final AccountRepository accountRepository;

    /**
     * Validate API key and return associated account.
     */
    public Optional<Account> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return accountRepository.findByApiKey(apiKey);
    }

    /**
     * Generate a new API key for an account.
     * Returns the generated key string.
     */
    public String generateApiKey(Account account) {
        String apiKey = UUID.randomUUID().toString();
        account.setApiKey(apiKey);
        accountRepository.save(account);
        return apiKey;
    }
}