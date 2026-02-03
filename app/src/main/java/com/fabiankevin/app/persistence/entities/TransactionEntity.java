package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.embeddables.AmountEmbeddable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
@Entity
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "accountId")
    private AccountEntity account;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @ManyToOne
    @JoinColumn(name = "categoryId")
    private CategoryEntity category;
    @Embedded
    private AmountEmbeddable amount;
    private String description;
    private LocalDate transactionDate;
    private Instant createdAt;
    private Instant updatedAt;

    public static TransactionEntity from(Transaction transaction) {
        if (transaction == null) return null;
        return TransactionEntity.builder()
                .id(transaction.id())
                .account(AccountEntity.from(transaction.account()))
                .type(transaction.type())
                .category(CategoryEntity.from(transaction.category()))
                .amount(AmountEmbeddable.from(transaction.amount()))
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
                .type(this.type)
                .category(Optional.ofNullable(this.category).map(CategoryEntity::toModel).orElse(null))
                .amount(Optional.ofNullable(this.amount).map(AmountEmbeddable::toModel).orElse(null))
                .description(this.description)
                .transactionDate(this.transactionDate)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
