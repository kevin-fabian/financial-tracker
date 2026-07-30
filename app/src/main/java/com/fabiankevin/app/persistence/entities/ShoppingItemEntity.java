package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@ToString(exclude = "shoppingList")
@EqualsAndHashCode(exclude = "shoppingList")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "shopping_items")
@Entity
public class ShoppingItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "quantity")
    private double quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "price")
    private double price;

    @Column(name = "purchased")
    private boolean purchased;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private ItemPriority priority;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingListEntity shoppingList;

    public static ShoppingItemEntity from(ShoppingItem item) {
        if (item == null) return null;
        return ShoppingItemEntity.builder()
                .id(item.id())
                .name(item.name())
                .category(item.category())
                .quantity(item.quantity())
                .unit(item.unit())
                .price(item.price())
                .purchased(item.purchased())
                .priority(item.priority())
                .notes(item.notes())
                .addedBy(item.addedBy())
                .createdAt(item.createdAt())
                .updatedAt(item.updatedAt())
                .build();
    }

    public ShoppingItem toModel() {
        return ShoppingItem.builder()
                .id(this.id)
                .name(this.name)
                .category(this.category)
                .quantity(this.quantity)
                .unit(this.unit)
                .price(this.price)
                .purchased(this.purchased)
                .priority(this.priority)
                .notes(this.notes)
                .addedBy(this.addedBy)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
