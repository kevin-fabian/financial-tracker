package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
                @Index(name = "idx_categories_name_transaction_type_user_id", columnList = "name, transaction_type, user_id", unique = true),
                @Index(name = "idx_categories_user_id", columnList = "user_id")
        })
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
    @Column(name = "icon", length = 128)
    private String icon;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "`system`", nullable = false)
    private boolean system = false;

    public static CategoryEntity from(Category category) {
        if (category == null) return null;
        return CategoryEntity.builder()
                .id(category.id())
                .name(category.name())
                .transactionType(category.type())
                .icon(category.icon())
                .userId(category.userId())
                .active(category.active())
                .system(category.system())
                .createdAt(category.createdAt())
                .updatedAt(category.updatedAt())
                .build();
    }

    public Category toModel() {
        return Category.builder()
                .id(this.id)
                .name(this.name)
                .type(this.transactionType)
                .icon(this.icon)
                .userId(this.userId)
                .active(this.active)
                .system(this.system)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}