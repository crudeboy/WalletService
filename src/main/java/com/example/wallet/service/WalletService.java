package com.example.wallet.service;

import com.example.wallet.entity.Transaction;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.TransactionRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public Wallet createWallet() {
        Wallet wallet = new Wallet();
        wallet.setBalance(0L); // initial balance in minor units
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet creditWallet(Long walletId, Long amount, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getWallet(); // idempotency: return previous result
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(TransactionType.CREDIT);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        return wallet;
    }

    @Transactional
    public Wallet debitWallet(Long walletId, Long amount, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getWallet();
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId);

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(TransactionType.DEBIT);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        return wallet;
    }

    @Transactional
    public void transfer(Long fromWalletId, Long toWalletId, Long amount, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return; // idempotency
        }

        Wallet sender = walletRepository.findByIdForUpdate(fromWalletId);
        Wallet receiver = walletRepository.findByIdForUpdate(toWalletId);

        if (sender.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        walletRepository.save(sender);
        walletRepository.save(receiver);

        Transaction tx = new Transaction();
        tx.setWallet(sender);
        tx.setRelatedWallet(receiver);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);
    }

    public Wallet get(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }
}