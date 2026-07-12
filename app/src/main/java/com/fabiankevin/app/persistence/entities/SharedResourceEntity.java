package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.shared_space.SharedResource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ToString(exclude = "sharedSpace")
@EqualsAndHashCode(exclude = "sharedSpace")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "shared_resources")
@Entity
public class SharedResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType type;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_id", columnDefinition = "json")
    private List<String> itemIds;

    @Column(name = "shared_by_owner")
    private boolean sharedByOwner;

    @Column(name = "shared_at")
    private Instant sharedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_space_id")
    private SharedSpaceEntity sharedSpace;

    public static SharedResourceEntity from(SharedResource resource) {
        if (resource == null) return null;
        return SharedResourceEntity.builder()
                .id(resource.id())
                .type(resource.type())
                .itemIds(resource.items())
                .sharedAt(resource.sharedAt())
                .build();
    }

    public SharedResource toModel() {
        return SharedResource.builder()
                .id(this.id)
                .type(this.type)
                .items(this.itemIds)
                .sharedAt(this.sharedAt)
                .build();
    }

}
