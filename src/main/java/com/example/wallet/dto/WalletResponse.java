package com.example.wallet.dto;

import com.example.wallet.entity.Wallet;

import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        Long balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // Static factory for mapping entity -> DTO
    public static WalletResponse fromEntity(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}