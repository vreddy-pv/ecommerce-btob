package com.btob.account.repository;

import com.btob.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Account entity.
 * Provides account lookup methods for authentication and API key validation.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find account by email for login.
     */
    Optional<Account> findByEmail(String email);

    /**
     * Find account by API key for service-to-service authentication.
     * Satisfies AUTH-03 (API keys for service-to-service authentication).
     */
    Optional<Account> findByApiKey(String apiKey);

    /**
     * Check if email already exists.
     */
    boolean existsByEmail(String email);
}