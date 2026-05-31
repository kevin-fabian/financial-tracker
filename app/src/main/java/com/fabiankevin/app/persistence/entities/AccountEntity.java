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
@Table(name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_name_user_id", columnList = "name, user_id", unique = true),
        })
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

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH})
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
                .type(Optional.ofNullable(this.type).map(AccountType::valueOf).orElse(null))
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .icon(Optional.ofNullable(this.icon).map(IconEntity::toModel).orElse(null))
                .build();
    }
}
