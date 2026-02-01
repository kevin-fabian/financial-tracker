package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.embeddables.AmountEmbeddable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
@Entity
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "accountId")
    private AccountEntity account;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @OneToOne(cascade = CascadeType.ALL)
    private CategoryEntity category;
    @Embedded
    private AmountEmbeddable amount;
    private String description;
    private LocalDate transactionDate;
    private Instant createdAt;
    private Instant updatedAt;
}
