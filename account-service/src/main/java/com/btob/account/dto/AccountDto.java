package com.btob.account.dto;

import com.btob.account.entity.AccountTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for account creation and updates.
 * Satisfies ACCT-01 (credit limits, balance) and ACCT-02 (tier determines pricing).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String companyName;

    @NotNull(message = "Tier is required")
    private AccountTier tier;

    private BigDecimal creditLimit;
}