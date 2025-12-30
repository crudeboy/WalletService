package com.example.wallet.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WalletEvent {
    private final Long walletId;
    private final Long relatedWalletId; // optional, for transfers
    private final Long amount;
    private final String type; // CREDIT, DEBIT, TRANSFER
}
