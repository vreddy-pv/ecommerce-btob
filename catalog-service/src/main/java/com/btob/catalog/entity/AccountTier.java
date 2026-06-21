package com.btob.catalog.entity;

/**
 * Account tier levels for B2B pricing.
 * Duplicated from account-service to avoid cross-service dependency.
 */
public enum AccountTier {
    STANDARD,
    SILVER,
    GOLD,
    PLATINUM
}
