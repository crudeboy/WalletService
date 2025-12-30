package com.example.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final WalletEventProducer walletEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletEvent(WalletEvent event) {
        log.info("Publishing wallet event to Kafka: {}", event);

        String key = "wallet-" + event.getWalletId();
        String payload = createPayload(event);

        walletEventProducer.publishWalletEvent(key, payload);
    }

    private String createPayload(WalletEvent event) {
        if ("TRANSFER".equals(event.getType())) {
            return """
            {
                "walletId": %d,
                "relatedWalletId": %d,
                "amount": %d,
                "type": "%s"
            }
            """.formatted(event.getWalletId(), event.getRelatedWalletId(), event.getAmount(), event.getType());
        } else {
            return """
            {
                "walletId": %d,
                "amount": %d,
                "type": "%s"
            }
            """.formatted(event.getWalletId(), event.getAmount(), event.getType());
        }
    }
}