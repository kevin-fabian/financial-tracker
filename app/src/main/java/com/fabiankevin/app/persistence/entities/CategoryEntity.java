package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
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
@Table(name = "categories",
        indexes = {
                @Index(name = "idx_categories_name_transaction_type", columnList = "name, transaction_type"),
                @Index(name = "idx_categories_user_id", columnList = "user_id")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "transaction_type", "user_id"}))
@Entity
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(length = 128)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public static CategoryEntity from(Category category) {
        if (category == null) return null;
        return CategoryEntity.builder()
                .id(category.id())
                .name(category.name())
                .transactionType(category.type())
                .userId(category.userId())
                .createdAt(category.createdAt())
                .updatedAt(category.updatedAt())
                .build();
    }

    public Category toModel() {
        return Category.builder()
                .id(this.id)
                .name(this.name)
                .type(this.transactionType)
                .userId(this.userId)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}