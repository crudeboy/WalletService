package com.example.wallet.controller;

import com.example.wallet.entity.Wallet;
import com.example.wallet.service.WalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public Wallet createWallet() {
        return walletService.createWallet();
    }

    @PostMapping("/{id}/credit")
    public Wallet credit(@PathVariable Long id,
                         @RequestParam Long amount,
                         @RequestParam String idempotencyKey) {
        return walletService.creditWallet(id, amount, idempotencyKey);
    }

    @PostMapping("/{id}/debit")
    public Wallet debit(@PathVariable Long id,
                        @RequestParam Long amount,
                        @RequestParam String idempotencyKey) {
        return walletService.debitWallet(id, amount, idempotencyKey);
    }

    @PostMapping("/transfer")
    public void transfer(@RequestParam Long fromWalletId,
                         @RequestParam Long toWalletId,
                         @RequestParam Long amount,
                         @RequestParam String idempotencyKey) {
        walletService.transfer(fromWalletId, toWalletId, amount, idempotencyKey);
    }

    @GetMapping("/{id}")
    public Wallet getWallet(@PathVariable Long id) {
        return walletService.get(id);
    }
}
