package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "recurring_transactions", indexes = {
        @Index(name = "idx_recurring_transactions_account_id", columnList = "account_id"),
        @Index(name = "idx_recurring_transactions_category_id", columnList = "category_id")
})
@Entity
public class RecurringTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(name = "next_occurrence_date")
    private ZonedDateTime nextOccurrenceDate;

    @Column(name = "start_date")
    private ZonedDateTime startDate;

    @Column(name = "end_date")
    private ZonedDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecurringTransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static RecurringTransactionEntity from(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) return null;
        return RecurringTransactionEntity.builder()
                .id(recurringTransaction.id())
                .description(recurringTransaction.description())
                .amount(recurringTransaction.amount())
                .transactionType(recurringTransaction.transactionType())
                .category(CategoryEntity.from(recurringTransaction.category()))
                .account(AccountEntity.from(recurringTransaction.account()))
                .dayOfMonth(recurringTransaction.dayOfMonth())
                .nextOccurrenceDate(recurringTransaction.nextOccurrenceDate())
                .startDate(recurringTransaction.startDate())
                .endDate(recurringTransaction.endDate())
                .status(recurringTransaction.status())
                .createdAt(recurringTransaction.createdAt())
                .updatedAt(recurringTransaction.updatedAt())
                .build();
    }

    public RecurringTransaction toModel() {
        return RecurringTransaction.builder()
                .id(this.id)
                .description(this.description)
                .amount(this.amount)
                .transactionType(this.transactionType)
                .category(this.category != null ? this.category.toModel() : null)
                .account(this.account != null ? this.account.toModel() : null)
                .dayOfMonth(this.dayOfMonth)
                .nextOccurrenceDate(this.nextOccurrenceDate)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .status(this.status)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
