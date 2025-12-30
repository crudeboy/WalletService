package com.example.wallet.service;

import com.example.wallet.dto.TransferResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.entity.Transaction;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.InsufficientBalanceException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.TransactionRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService2 {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletEventProducer walletEventProducer;
    private final ApplicationEventPublisher eventPublisher; // Spring event publisher


    public WalletResponse createWallet() {
        log.info("Wallet creation request received.");
        Wallet wallet = new Wallet();
        wallet.setBalance(0L); // initial balance in minor units
        Wallet saved = walletRepository.save(wallet);

        log.info("Wallet created [walletId={}, balance={}]",
                saved.getId(), saved.getBalance());

        return WalletResponse.fromEntity(saved);
    }

    @Transactional
    public WalletResponse creditWallet(Long walletId, Long amount, String idempotencyKey) {
        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return WalletResponse.fromEntity(existing.get().getWallet());
        }

        Wallet wallet = applyCredit(walletId, amount);

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(TransactionType.CREDIT);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        // Publish domain event
        eventPublisher.publishEvent(new WalletEvent(walletId, null, amount, "CREDIT"));

        return WalletResponse.fromEntity(wallet);
    }

    @Transactional
    public WalletResponse debitWallet(Long walletId, Long amount, String idempotencyKey) {
        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return WalletResponse.fromEntity(existing.get().getWallet());
        }

        Wallet wallet = applyDebit(walletId, amount);

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(TransactionType.DEBIT);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        // Publish domain event
        eventPublisher.publishEvent(new WalletEvent(walletId, null, amount, "DEBIT"));

        return WalletResponse.fromEntity(wallet);
    }


    @Transactional
    public TransferResponse transfer(Long fromWalletId, Long toWalletId, Long amount, String idempotencyKey) {
        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return TransferResponse.fromTransaction(existing.get());
        }

        Wallet sender = applyDebit(fromWalletId, amount);
        Wallet receiver = applyCredit(toWalletId, amount);

        Transaction tx = new Transaction();
        tx.setWallet(sender);
        tx.setRelatedWallet(receiver);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        // Publish transfer event
        eventPublisher.publishEvent(new WalletEvent(fromWalletId, toWalletId, amount, "TRANSFER"));

        return TransferResponse.fromTransaction(tx);
    }

    public WalletResponse get(Long walletId) {
        log.info("Retrieving Wallet info for : {}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException(walletId));
        return WalletResponse.fromEntity(wallet);
    }

    // applyCredit / applyDebit just update DB
    private Wallet applyCredit(Long walletId, Long amount) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId);
        if (wallet == null) throw new WalletNotFoundException(walletId);

        wallet.setBalance(wallet.getBalance() + amount);
        return walletRepository.save(wallet);
    }

    private Wallet applyDebit(Long walletId, Long amount) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId);
        if (wallet == null) throw new WalletNotFoundException(walletId);

        if (wallet.getBalance() < amount) {
            throw new InsufficientBalanceException(walletId, wallet.getBalance(), amount);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        return walletRepository.save(wallet);
    }

}