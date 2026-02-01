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
@Table(name = "categories")
@Entity
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, length = 128)
    private String name;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public static CategoryEntity from(Category category) {
        if (category == null) return null;
        return CategoryEntity.builder()
                .id(category.id())
                .name(category.name())
                .createdAt(category.createdAt())
                .updatedAt(category.updatedAt())
                .build();
    }

    public Category toModel() {
        return Category.builder()
                .id(this.id)
                .name(this.name)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}