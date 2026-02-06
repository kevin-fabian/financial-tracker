package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Category;
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
                @Index(name = "idx_categories_name", columnList = "name"),
                @Index(name = "idx_categories_user_id", columnList = "user_id")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "user_id"}))
@Entity
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(length = 128)
    private String name;
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
                .userId(category.userId())
                .createdAt(category.createdAt())
                .updatedAt(category.updatedAt())
                .build();
    }

    public Category toModel() {
        return Category.builder()
                .id(this.id)
                .name(this.name)
                .userId(this.userId)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}