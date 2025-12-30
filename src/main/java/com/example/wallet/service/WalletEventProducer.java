package com.example.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    public static final String TOPIC =  "wallet-transactions";


    public static String createEventString(long walletId, long amount, String type) {
        return """
            {
              "walletId": %d,
              "amount": %d,
              "type": "%s"
            }
            """.formatted(walletId, amount, type);
    }

    public static String createTransferEventString(long fromWalletId, long toWalletId, long amount) {
        return  """
                    {
                      "fromWalletId": %d,
                      "toWalletId": %d,
                      "amount": %d,
                      "type": "TRANSFER"
                    }
                    """.formatted(fromWalletId, toWalletId, amount);
    }

    public void publishWalletEvent(String key, String payload) {
        kafkaTemplate.send(TOPIC, key, payload);
    }
}
