package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.party.ResourceType;
import com.fabiankevin.app.models.party.SharedItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ToString(exclude = "party")
@EqualsAndHashCode(exclude = "party")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "shared_items")
@Entity
public class SharedItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_id", columnDefinition = "json")
    private List<String> itemIds;

    @Column(name = "shared_at")
    private Instant sharedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private PartyEntity party;

    public static SharedItemEntity from(SharedItem resource) {
        if (resource == null) return null;
        return SharedItemEntity.builder()
                .id(resource.id())
                .type(resource.type())
                .itemIds(resource.items())
                .sharedAt(resource.sharedAt())
                .build();
    }

    public SharedItem toModel() {
        return SharedItem.builder()
                .id(this.id)
                .type(this.type)
                .items(this.itemIds)
                .sharedAt(this.sharedAt)
                .build();
    }

}
