package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "shopping_lists")
@Entity
public class ShoppingListEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShoppingListStatus status;

    @Column(name = "user_id")
    private UUID userId;

    @ElementCollection
    @CollectionTable(
            name = "shopping_lists_shared_users",
            joinColumns = @JoinColumn(name = "shopping_item_id")
    )
    @Column(name = "shared_user_id")
    private List<UUID> sharedWithUserIds = new ArrayList<>();

    @Column(name = "budget")
    private double budget;

    @Column(name = "final_amount")
    private Double finalAmount;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ShoppingItemEntity> items = new HashSet<>();

    public void addItem(ShoppingItemEntity item) {
        if (item == null) return;
        item.setShoppingList(this);
        this.items.add(item);
    }

    public static ShoppingListEntity from(ShoppingList shoppingList) {
        if (shoppingList == null) return null;
        ShoppingListEntity entity = ShoppingListEntity.builder()
                .id(shoppingList.id())
                .name(shoppingList.name())
                .description(shoppingList.description())
                .status(shoppingList.status())
                .userId(shoppingList.userId())
                .sharedWithUserIds(shoppingList.sharedWithUserIds())
                .budget(shoppingList.budget())
                .finalAmount(shoppingList.finalAmount())
                .completedAt(shoppingList.completedAt())
                .createdAt(shoppingList.createdAt())
                .updatedAt(shoppingList.updatedAt())
                .items(new HashSet<>())
                .build();

        for (ShoppingItem shoppingItem : shoppingList.items()) {
            entity.addItem(ShoppingItemEntity.from(shoppingItem));
        }

        return entity;
    }

    public ShoppingList toModel() {
        return ShoppingList.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .status(this.status)
                .userId(this.userId)
                .sharedWithUserIds(this.sharedWithUserIds != null ? new ArrayList<>(this.sharedWithUserIds) : new ArrayList<>())
                .budget(this.budget)
                .finalAmount(this.finalAmount)
                .completedAt(this.completedAt)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .items(this.items.stream().map(ShoppingItemEntity::toModel).collect(ArrayList::new, ArrayList::add, ArrayList::addAll))
                .build();
    }
}
