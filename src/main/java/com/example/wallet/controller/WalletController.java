package com.example.wallet.controller;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.dto.WalletTransactionRequest;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public WalletResponse createWallet() {
        return walletService.createWallet();
    }

    @PostMapping("/{id:\\d+}/credit")
    public WalletResponse credit(
            @PathVariable @Min(value = 1, message = "Wallet ID must be positive") Long id,
            @Valid @RequestBody WalletTransactionRequest request
    ) {
        return walletService.creditWallet(id, request.amount(), request.idempotencyKey());
    }

    @PostMapping("/{id:\\d+}/debit")
    public WalletResponse debit(
            @PathVariable Long id,
            @Valid @RequestBody WalletTransactionRequest request
    ) {
        return walletService.debitWallet(id, request.amount(), request.idempotencyKey());
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        return walletService.transfer(
                request.fromWalletId(),
                request.toWalletId(),
                request.amount(),
                request.idempotencyKey()
        );
    }

    @GetMapping("/{id:\\d+}")
    public WalletResponse getWallet(@PathVariable @Min(value = 1, message = "Wallet ID must be positive") Long id) {
        return walletService.get(id);
    }
}
