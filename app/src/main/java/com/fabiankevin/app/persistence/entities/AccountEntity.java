package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.Account;
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
@Table(name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_name", columnList = "name"),
                @Index(name = "idx_accounts_user_id", columnList = "user_id")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "user_id"}))
@Entity
public class AccountEntity{
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_id", nullable = true)
    private IconEntity icon;

    public static AccountEntity from(Account account) {
        if (account == null) return null;
        return AccountEntity.builder()
                .id(account.id())
                .name(account.name())
                .userId(account.userId())
                .currency(Optional.ofNullable(account.currency()).map(Currency::getCurrencyCode).orElse(null))
                .type(account.type() != null ? account.type().name() : null)
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .icon(account.icon() != null ? IconEntity.from(account.icon()) : null)
                .build();
    }

    public Account toModel() {
        return Account.builder()
                .id(this.id)
                .name(this.name)
                .userId(this.userId)
                .currency(Optional.ofNullable(this.currency).map(Currency::getInstance).orElse(null))
                .type(Optional.ofNullable(this.type).map(s -> com.fabiankevin.app.models.enums.AccountType.valueOf(s)).orElse(null))
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .icon(this.icon != null ? this.icon.toModel() : null)
                .build();
    }
}
