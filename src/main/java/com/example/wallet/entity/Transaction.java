package com.example.wallet.entity;

import com.example.wallet.type.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "transactions",
        indexes = @Index(name = "idx_wallet_id", columnList = "wallet_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Long amount; // minor units

    @Column(nullable = false)
    private String idempotencyKey;

    @ManyToOne
    @JoinColumn(name = "related_wallet_id")
    private Wallet relatedWallet; // for transfers

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
