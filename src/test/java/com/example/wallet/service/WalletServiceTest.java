package com.example.wallet.service;

import com.example.wallet.dto.TransferResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.exception.InsufficientBalanceException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // forces Spring to load application-test.properties
public class WalletServiceTest {

    @Autowired
    WalletService walletService;

    @Test
    void shouldCreateWalletWithZeroBalance() {
        WalletResponse wallet = walletService.createWallet();
        assertNotNull(wallet.id());
        assertEquals(0L, wallet.balance());
    }


    @Test
    void shouldCreditWalletSuccessfully() {
        WalletResponse wallet = walletService.createWallet();
        WalletResponse credited = walletService.creditWallet(wallet.id(), 1000L, UUID.randomUUID().toString());

        assertEquals(1000L, credited.balance());
    }

    @Test
    void shouldDebitWalletSuccessfully() {
        WalletResponse wallet = walletService.createWallet();
        String key1 = UUID.randomUUID().toString();
        walletService.creditWallet(wallet.id(), 1000L, key1);

        WalletResponse debited = walletService.debitWallet(wallet.id(), 500L, UUID.randomUUID().toString());

        assertEquals(500L, debited.balance());
    }

    @Test
    void shouldFailDebitWhenInsufficientBalance() {
        WalletResponse wallet = walletService.createWallet();

        assertThrows(InsufficientBalanceException.class, () ->
                walletService.debitWallet(wallet.id(), 100L, UUID.randomUUID().toString())
        );
    }

    @Test
    void shouldNotDuplicateCreditWithSameIdempotencyKey() {
        WalletResponse wallet = walletService.createWallet();
        String key = UUID.randomUUID().toString();

        WalletResponse first = walletService.creditWallet(wallet.id(), 1000L, key);
        WalletResponse second = walletService.creditWallet(wallet.id(), 1000L, key);

        assertEquals(1000L, first.balance());
        assertEquals(first.balance(), second.balance());
    }

    @Test
    void shouldNotDuplicateDebitWithSameIdempotencyKey() {
        WalletResponse wallet = walletService.createWallet();
        String creditKey = UUID.randomUUID().toString();
        walletService.creditWallet(wallet.id(), 1000L, creditKey);

        String debitKey = UUID.randomUUID().toString();
        WalletResponse first = walletService.debitWallet(wallet.id(), 500L, debitKey);
        WalletResponse second = walletService.debitWallet(wallet.id(), 500L, debitKey);

        assertEquals(500L, first.balance());
        assertEquals(first.balance(), second.balance());
    }

    @Test
    void shouldTransferBetweenWalletsSuccessfully() {
        WalletResponse sender = walletService.createWallet();
        WalletResponse receiver = walletService.createWallet();

        walletService.creditWallet(sender.id(), 1000L, UUID.randomUUID().toString());
        TransferResponse transfer = walletService.transfer(sender.id(), receiver.id(), 400L, UUID.randomUUID().toString());

        WalletResponse updatedSender = walletService.get(sender.id());
        WalletResponse updatedReceiver = walletService.get(receiver.id());

        assertEquals(600L, updatedSender.balance());
        assertEquals(400L, updatedReceiver.balance());
        assertEquals(400L, transfer.amount());
    }

    @Test
    void shouldFailTransferIfInsufficientBalance() {
        WalletResponse sender = walletService.createWallet();
        WalletResponse receiver = walletService.createWallet();

        assertThrows(InsufficientBalanceException.class, () ->
                walletService.transfer(sender.id(), receiver.id(), 100L, UUID.randomUUID().toString())
        );
    }

    @Test
    void shouldNotDuplicateTransferWithSameIdempotencyKey() {
        WalletResponse sender = walletService.createWallet();
        WalletResponse receiver = walletService.createWallet();
        walletService.creditWallet(sender.id(), 1000L, UUID.randomUUID().toString());

        String key = UUID.randomUUID().toString();
        TransferResponse first = walletService.transfer(sender.id(), receiver.id(), 200L, key);
        TransferResponse second = walletService.transfer(sender.id(), receiver.id(), 200L, key);

        assertEquals(first.amount(), second.amount());
    }
}
