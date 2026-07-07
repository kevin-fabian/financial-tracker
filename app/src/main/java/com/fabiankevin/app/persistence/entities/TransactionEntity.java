package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_account_id", columnList = "account_id"),
        @Index(name = "idx_transactions_category_id", columnList = "category_id")
})
@Entity
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private AccountEntity account;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;
    @Column(nullable = false)
    private double amount;
    @Column(nullable = false, length = 3)
    private String currency;
    private String description;
    private LocalDate transactionDate;
    private Instant createdAt;
    private Instant updatedAt;

    public static TransactionEntity from(Transaction transaction) {
        if (transaction == null) return null;
        return TransactionEntity.builder()
                .id(transaction.id())
                .account(AccountEntity.from(transaction.account()))
                .category(CategoryEntity.from(transaction.category()))
                .amount(transaction.amount().value())
                .currency(transaction.amount().currency().getCurrencyCode())
                .description(transaction.description())
                .transactionDate(transaction.transactionDate())
                .createdAt(transaction.createdAt())
                .updatedAt(transaction.updatedAt())
                .build();
    }

    public Transaction toModel() {
        return Transaction.builder()
                .id(this.id)
                .account(Optional.ofNullable(this.account).map(AccountEntity::toModel).orElse(null))
                .type(Optional.ofNullable(this.category).map(CategoryEntity::toModel).map(Category::type).orElse(null))
                .category(Optional.ofNullable(this.category).map(CategoryEntity::toModel).orElse(null))
                .amount(Amount.of(
                        this.amount,
                        Currency.getInstance(this.currency)
                ))
                .description(this.description)
                .transactionDate(this.transactionDate)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
