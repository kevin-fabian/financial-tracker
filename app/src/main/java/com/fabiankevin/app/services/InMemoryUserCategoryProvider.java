package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
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
            Map.entry("groceries", List.of(
                    Category.builder().name("Groceries").type(EXPENSE).userId(null).icon("local_grocery_store").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("bills", List.of(
                    Category.builder().name("Utilities").type(EXPENSE).userId(null).icon("bolt").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                    Category.builder().name("Subscriptions").type(EXPENSE).userId(null).icon("card_membership").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("rent", List.of(
                    Category.builder().name("Housing & Rent").type(EXPENSE).userId(null).icon("home").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                    Category.builder().name("Home Maintenance").type(EXPENSE).userId(null).icon("build").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("entertainment", List.of(
                    Category.builder().name("Entertainment & Hobbies").type(EXPENSE).userId(null).icon("sports_esports").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("savings", List.of(
                    Category.builder().name("Savings & Goals").type(EXPENSE).userId(null).icon("savings").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("shopping", List.of(
                    Category.builder().name("Shopping").type(EXPENSE).userId(null).icon("shopping_bag").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                    Category.builder().name("Personal Care").type(EXPENSE).userId(null).icon("face").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("health_fitness", List.of(
                    Category.builder().name("Healthcare").type(EXPENSE).userId(null).icon("medical_services").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                    Category.builder().name("Fitness & Wellness").type(EXPENSE).userId(null).icon("fitness_center").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("family_pets", List.of(
                    Category.builder().name("Family & Kids").type(EXPENSE).userId(null).icon("family_restroom").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                    Category.builder().name("Pets").type(EXPENSE).userId(null).icon("pets").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("debt_loans", List.of(
                    Category.builder().name("Debt & Loans").type(EXPENSE).userId(null).icon("credit_card").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                    Category.builder().name("Installments & Amortization").type(EXPENSE).userId(null).icon("receipt_long").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),

//          INCOME
            Map.entry("salary_active", List.of(
                    Category.builder().name("Salary").type(INCOME).userId(null).icon("payments").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("business_sales", List.of(
                    Category.builder().name("Business & Sales").type(INCOME).userId(null).icon("storefront").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("passive_investments", List.of(
                    Category.builder().name("Investments").type(INCOME).userId(null).icon("trending_up").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("allowances_gifts", List.of(
                    Category.builder().name("Gifts & Allowances").type(INCOME).userId(null).icon("card_giftcard").active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            ))
    );
}
