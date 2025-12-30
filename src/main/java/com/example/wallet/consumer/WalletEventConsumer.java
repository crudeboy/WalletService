package com.example.wallet.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WalletEventConsumer {

    @KafkaListener(topics = "wallet-transactions", groupId = "wallet-consumer-group")
    public void consume(String message) {
        log.info("Received Wallet Event: {}", message);

        // Here you could parse JSON and take action
        // Example: parse and print walletId, amount, type
        try {
            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(message);
            long walletId = json.get("walletId").asLong();
            long amount = json.get("amount").asLong();
            String type = json.get("type").asText();

            log.info("Parsed Event -> walletId: {}, amount: {}, type: {}", walletId, amount, type);

        } catch (Exception e) {
            log.error("Failed to parse wallet event", e);
        }
    }
}