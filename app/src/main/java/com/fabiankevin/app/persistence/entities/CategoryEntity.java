package com.fabiankevin.app.persistence.entities;

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
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
}