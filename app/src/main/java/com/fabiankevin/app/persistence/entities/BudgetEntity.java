package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "budgets", indexes = {
        @Index(name = "idx_budgets_user_id", columnList = "user_id"),
        @Index(name = "idx_budgets_category_id", columnList = "category_id")
})
@Entity
public class BudgetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BudgetPeriod period;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(nullable = false)
    private double allocated;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static BudgetEntity from(Budget budget) {
        if (budget == null) return null;
        return BudgetEntity.builder()
                .id(budget.id())
                .userId(budget.user().id())
                .updatedBy(budget.updatedBy() != null ? budget.updatedBy().id() : null)
                .period(budget.period())
                .category(CategoryEntity.from(budget.category()))
                .allocated(budget.allocated())
                .createdAt(budget.createdAt())
                .updatedAt(budget.updatedAt())
                .build();
    }

    public Budget toModel() {
        return new Budget(
                this.id,
                User.of(this.userId),
                this.updatedBy != null ? User.of(this.updatedBy) : null,
                this.period,
                this.category != null ? this.category.toModel() : null,
                this.allocated,
                this.createdAt,
                this.updatedAt
        );
    }
}
