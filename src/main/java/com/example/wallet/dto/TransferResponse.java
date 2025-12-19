package com.example.wallet.dto;

import com.example.wallet.entity.Transaction;

import java.time.LocalDateTime;

public record TransferResponse(
        Long fromWalletId,
        Long toWalletId,
        Long amount,           // minor units
        LocalDateTime timestamp
) {
    public static TransferResponse fromTransaction(Transaction tx) {
        return new TransferResponse(
                tx.getWallet().getId(),          // sender
                tx.getRelatedWallet().getId(),   // receiver
                tx.getAmount(),
                tx.getCreatedAt()
        );
    }
}
