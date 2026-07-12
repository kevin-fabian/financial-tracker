package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.shared_space.SharedResource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @ElementCollection
    @CollectionTable(name = "shared_resource_item_ids", joinColumns = @JoinColumn(name = "shared_resource_id"))
    @Column(name = "item_id")
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
                .resourceName(resource.resourceName())
                .ownerUserId(resource.ownerUserId())
                .itemIds(resource.itemIds())
                .sharedByOwner(resource.sharedByOwner())
                .sharedAt(resource.sharedAt())
                .build();
    }

    public SharedResource toModel() {
        return SharedResource.builder()
                .id(this.id)
                .type(this.type)
                .resourceName(this.resourceName)
                .ownerUserId(this.ownerUserId)
                .itemIds(this.itemIds)
                .sharedByOwner(this.sharedByOwner)
                .sharedAt(this.sharedAt)
                .build();
    }

}
