package com.example.wallet.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long walletId, Long balance, Long amount) {
        super(String.format(
                "Wallet %d has insufficient balance. Balance=%d, Requested=%d",
                walletId, balance, amount
        ));
    }
}
