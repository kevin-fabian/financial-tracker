package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "accounts")
@Entity
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @Column(name = "user_id")
    private UUID userId;
    private String currency;
    private String type;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Column(nullable = false)
    private boolean active = true;

    public static AccountEntity from(Account account) {
        if (account == null) return null;
        return AccountEntity.builder()
                .id(account.id())
                .name(account.name())
                .userId(account.userId())
                .currency(Optional.ofNullable(account.currency()).map(Currency::getCurrencyCode).orElse(null))
                .type(account.type() != null ? account.type().name() : null)
                .active(account.active())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .build();
    }

    public Account toModel() {
        return Account.builder()
                .id(this.id)
                .name(this.name)
                .userId(this.userId)
                .currency(Optional.ofNullable(this.currency).map(Currency::getInstance).orElse(null))
                .type(Optional.ofNullable(this.type).map(AccountType::valueOf).orElse(null))
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
