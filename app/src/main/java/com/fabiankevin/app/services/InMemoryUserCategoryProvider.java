package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static com.fabiankevin.app.models.enums.TransactionType.INCOME;

@Component
public class InMemoryUserCategoryProvider implements UserCategoryProvider {
    @Override
    public List<Category> provide(Set<String> categoryInterests, UUID userId) {
        if (categoryInterests == null || categoryInterests.isEmpty()) {
            return Collections.emptyList();
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return categoryInterests.stream()
                .filter(CATEGORY_INTERESTS_MAPPING::containsKey)
                .flatMap(interest -> CATEGORY_INTERESTS_MAPPING.get(interest).stream())
                .map(category -> category.withUserId(userId))
                .toList();
    }

    private static final Map<String, List<Category>> CATEGORY_INTERESTS_MAPPING = Map.ofEntries(
            Map.entry("groceries", List.of(buildCategory("Groceries", EXPENSE, "local_grocery_store"))),
            Map.entry("bills", List.of(
                    buildCategory("Utilities", EXPENSE, "bolt"),
                    buildCategory("Subscriptions", EXPENSE, "card_membership")
            )),
            Map.entry("rent", List.of(
                    buildCategory("Housing & Rent", EXPENSE, "home"),
                    buildCategory("Home Maintenance", EXPENSE, "build")
            )),
            Map.entry("entertainment", List.of(
                    buildCategory("Entertainment & Hobbies", EXPENSE, "sports_esports")
            )),
            Map.entry("savings", List.of(
                    buildCategory("Savings & Goals", EXPENSE, "savings")
            )),
            Map.entry("shopping", List.of(
                    buildCategory("Shopping", EXPENSE, "shopping_bag"),
                    buildCategory("Personal Care", EXPENSE, "face")
            )),
            Map.entry("health_fitness", List.of(
                    buildCategory("Healthcare", EXPENSE, "medical_services"),
                    buildCategory("Fitness & Wellness", EXPENSE, "fitness_center")
            )),
            Map.entry("family_pets", List.of(
                    buildCategory("Family & Kids", EXPENSE, "family_restroom"),
                    buildCategory("Pets", EXPENSE, "pets")
            )),
            Map.entry("debt_loans", List.of(
                    buildCategory("Debt & Loans", EXPENSE, "credit_card"),
                    buildCategory("Installments & Amortization", EXPENSE, "receipt_long")
            )),
            Map.entry("salary_active", List.of(
                    buildCategory("Salary", INCOME, "payments")
            )),
            Map.entry("business_sales", List.of(
                    buildCategory("Business & Sales", INCOME, "storefront")
            )),
            Map.entry("passive_investments", List.of(
                    buildCategory("Investments", INCOME, "trending_up")
            )),
            Map.entry("allowances_gifts", List.of(
                    buildCategory("Gifts & Allowances", INCOME, "card_giftcard")
            ))
    );

    private static Category buildCategory(String name, TransactionType type, String icon) {
        return Category.builder()
                .name(name)
                .type(type)
                .userId(UUID.randomUUID())
                .icon(icon)
                .active(true)
                .system(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
