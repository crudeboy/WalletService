package com.example.wallet.service;

import com.example.wallet.dto.TransferResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.exception.InsufficientBalanceException;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
public class WalletServiceTest {

    @Autowired
    WalletService walletService;

    @Autowired
    private WalletEventProducer walletEventProducer;


    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0");


    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        kafka.waitingFor(Wait.forListeningPort()); // ensure container is ready
        System.out.println("**************** Bootstrap servers: " + kafka.getBootstrapServers());
        String bootstrapServers = kafka.getBootstrapServers().replace("PLAINTEXT://", "");
        System.out.println("Kafka bootstrap: " + bootstrapServers);
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
    }

    private KafkaConsumer<String, String> createKafkaConsumer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    private void assertKafkaEventExists(String topic, long walletId, long amount, String type) throws Exception {
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(kafka.getBootstrapServers())) {
            consumer.subscribe(Collections.singleton(topic));

            long timeout = System.currentTimeMillis() + 5000;
            boolean found = false;

            while (System.currentTimeMillis() < timeout && !found) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    JsonNode json = new ObjectMapper().readTree(record.value());
                    JsonNode walletNode = json.get("walletId");
                    JsonNode amountNode = json.get("amount");
                    JsonNode typeNode = json.get("type");

                    if (walletNode == null || amountNode == null || typeNode == null) {
                        System.out.println("Skipping unexpected record: " + record.value());
                        continue;
                    }

                    if (walletNode.asLong() == walletId &&
                            amountNode.asLong() == amount &&
                            typeNode.asText().equals(type)) {
                        found = true;
                        break;
                    }
                }
            }

            assertTrue(found, "Expected Kafka event for walletId=" + walletId + " type=" + type + " amount=" + amount);
        }
    }

    @Test
    void shouldCreateWalletWithZeroBalance() {
        WalletResponse wallet = walletService.createWallet();
        assertNotNull(wallet.id());
        assertEquals(0L, wallet.balance());
        // wallet creation may not emit Kafka, skip Kafka check here
    }

    @Test
    void shouldCreditWalletSuccessfullyAndPublishKafkaEvent() throws Exception {
        WalletResponse wallet = walletService.createWallet();
        String key = UUID.randomUUID().toString();
        WalletResponse credited = walletService.creditWallet(wallet.id(), 1000L, key);

        assertEquals(1000L, credited.balance());

        assertKafkaEventExists(WalletEventProducer.TOPIC, wallet.id(), 1000L, "CREDIT");
    }

    @Test
    void shouldDebitWalletSuccessfullyAndPublishKafkaEvent() throws Exception {
        WalletResponse wallet = walletService.createWallet();
        String creditKey = UUID.randomUUID().toString();
        walletService.creditWallet(wallet.id(), 1000L, creditKey);

        String debitKey = UUID.randomUUID().toString();
        WalletResponse debited = walletService.debitWallet(wallet.id(), 500L, debitKey);

        assertEquals(500L, debited.balance());

        assertKafkaEventExists(WalletEventProducer.TOPIC, wallet.id(), 500L, "DEBIT");
    }

    @Test
    void shouldFailDebitWhenInsufficientBalance() {
        WalletResponse wallet = walletService.createWallet();
        assertThrows(InsufficientBalanceException.class, () ->
                walletService.debitWallet(wallet.id(), 100L, UUID.randomUUID().toString())
        );
    }

    @Test
    void shouldNotDuplicateCreditWithSameIdempotencyKey() throws Exception {
        WalletResponse wallet = walletService.createWallet();
        String key = UUID.randomUUID().toString();

        WalletResponse first = walletService.creditWallet(wallet.id(), 1000L, key);
        WalletResponse second = walletService.creditWallet(wallet.id(), 1000L, key);

        assertEquals(1000L, first.balance());
        assertEquals(first.balance(), second.balance());

        assertKafkaEventExists(WalletEventProducer.TOPIC, wallet.id(), 1000L, "CREDIT");
    }

    @Test
    void shouldNotDuplicateDebitWithSameIdempotencyKey() throws Exception {
        WalletResponse wallet = walletService.createWallet();
        String creditKey = UUID.randomUUID().toString();
        walletService.creditWallet(wallet.id(), 1000L, creditKey);

        String debitKey = UUID.randomUUID().toString();
        WalletResponse first = walletService.debitWallet(wallet.id(), 500L, debitKey);
        WalletResponse second = walletService.debitWallet(wallet.id(), 500L, debitKey);

        assertEquals(500L, first.balance());
        assertEquals(first.balance(), second.balance());

        assertKafkaEventExists(WalletEventProducer.TOPIC, wallet.id(), 500L, "DEBIT");
    }

    @Test
    @Disabled
    void shouldTransferBetweenWalletsSuccessfullyAndPublishKafkaEvent() throws Exception {
        WalletResponse sender = walletService.createWallet();
        WalletResponse receiver = walletService.createWallet();
        walletService.creditWallet(sender.id(), 1000L, UUID.randomUUID().toString());

        String transferKey = UUID.randomUUID().toString();
        TransferResponse transfer = walletService.transfer(sender.id(), receiver.id(), 400L, transferKey);

        WalletResponse updatedSender = walletService.get(sender.id());
        WalletResponse updatedReceiver = walletService.get(receiver.id());

        assertEquals(600L, updatedSender.balance());
        assertEquals(400L, updatedReceiver.balance());
        assertEquals(400L, transfer.amount());

        // Assert Kafka events for both wallets
//        assertKafkaEventExists(WalletEventProducer.TOPIC, sender.id(), 400L, "TRANSFER", receiver.id());
//        assertKafkaEventExists(WalletEventProducer.TOPIC, receiver.id(), 400L, "CREDIT");
    }

    @Test
    @Disabled
    void shouldFailTransferIfInsufficientBalance() {
        WalletResponse sender = walletService.createWallet();
        WalletResponse receiver = walletService.createWallet();

        assertThrows(InsufficientBalanceException.class, () ->
                walletService.transfer(sender.id(), receiver.id(), 100L, UUID.randomUUID().toString())
        );
    }

    @Test
    @Disabled
    void shouldNotDuplicateTransferWithSameIdempotencyKey() throws Exception {
        WalletResponse sender = walletService.createWallet();
        WalletResponse receiver = walletService.createWallet();
        walletService.creditWallet(sender.id(), 1000L, UUID.randomUUID().toString());

        String key = UUID.randomUUID().toString();
        TransferResponse first = walletService.transfer(sender.id(), receiver.id(), 200L, key);
        TransferResponse second = walletService.transfer(sender.id(), receiver.id(), 200L, key);

        assertEquals(first.amount(), second.amount());

        // Assert Kafka events only once per transfer
//        assertKafkaEventExists(WalletEventProducer.TOPIC, sender.id(), 200L, "DEBIT");
//        assertKafkaEventExists(WalletEventProducer.TOPIC, receiver.id(), 200L, "CREDIT");
//        assertKafkaEventExists(WalletEventProducer.TOPIC, sender.id(), 400L, "TRANSFER", receiver.id());
    }
}

