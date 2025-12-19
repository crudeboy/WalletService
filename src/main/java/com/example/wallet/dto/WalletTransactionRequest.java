package com.example.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WalletTransactionRequest(
        @Min(value = 1, message = "Amount must be positive")
        Long amount,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {}
