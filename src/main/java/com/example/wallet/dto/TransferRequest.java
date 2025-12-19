package com.example.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull(message = "From wallet id is required")
        Long fromWalletId,

        @NotNull(message = "To wallet id is required")
        Long toWalletId,

        @Min(value = 1, message = "Amount must be positive")
        Long amount,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {}