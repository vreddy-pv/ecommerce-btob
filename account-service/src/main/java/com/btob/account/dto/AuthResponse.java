package com.btob.account.dto;

import com.btob.account.entity.AccountTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for authentication.
 * Satisfies AUTH-01 (login returns JWT) and AUTH-02 (JWT persists across refresh).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UUID accountId;
    private String email;
    private AccountTier tier;
    private Long expiresIn;
}