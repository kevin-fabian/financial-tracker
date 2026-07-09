package com.fabiankevin.app.services;

import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static com.fabiankevin.app.models.enums.TransactionType.INCOME;

@Component
@RequiredArgsConstructor
public class InMemoryUserCategoryProvider implements UserCategoryProvider {
    private final CategoryService categoryService;

    @Override
    public void provide(Set<String> categoryInterests, UUID userId) {
        if (categoryInterests == null || categoryInterests.isEmpty()) {
            return;
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        categoryInterests.stream()
                .filter(CATEGORY_INTERESTS_MAPPING::containsKey)
                .flatMap(interest -> CATEGORY_INTERESTS_MAPPING.get(interest).stream())
                .forEach(command -> categoryService.createCategory(command.toBuilder().userId(userId).build()));
    }

    private static final Map<String, List<CreateCategoryCommand>> CATEGORY_INTERESTS_MAPPING = Map.ofEntries(
            Map.entry("groceries", List.of(CreateCategoryCommand.builder()
                    .name("Groceries")
                    .type(EXPENSE)
                    .icon("local_grocery_store")
                    .build())),
            Map.entry("bills", List.of(
                    CreateCategoryCommand.builder()
                            .name("Utilities")
                            .type(EXPENSE)
                            .icon("bolt")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Subscriptions")
                            .type(EXPENSE)
                            .icon("card_membership")
                            .build())),
            Map.entry("rent", List.of(
                    CreateCategoryCommand.builder()
                            .name("Housing & Rent")
                            .type(EXPENSE)
                            .icon("home")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Home Maintenance")
                            .type(EXPENSE)
                            .icon("build")
                            .build())),
            Map.entry("entertainment", List.of(CreateCategoryCommand.builder()
                    .name("Entertainment & Hobbies")
                    .type(EXPENSE)
                    .icon("sports_esports")
                    .build())),
            Map.entry("savings", List.of(CreateCategoryCommand.builder()
                    .name("Savings & Goals")
                    .type(EXPENSE)
                    .icon("savings")
                    .build())),
            Map.entry("shopping", List.of(
                    CreateCategoryCommand.builder()
                            .name("Shopping")
                            .type(EXPENSE)
                            .icon("shopping_bag")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Personal Care")
                            .type(EXPENSE)
                            .icon("face")
                            .build())),
            Map.entry("health_fitness", List.of(
                    CreateCategoryCommand.builder()
                            .name("Healthcare")
                            .type(EXPENSE)
                            .icon("medical_services")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Fitness & Wellness")
                            .type(EXPENSE)
                            .icon("fitness_center")
                            .build())),
            Map.entry("family_pets", List.of(
                    CreateCategoryCommand.builder()
                            .name("Family & Kids")
                            .type(EXPENSE)
                            .icon("family_restroom")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Pets")
                            .type(EXPENSE)
                            .icon("pets")
                            .build())),
            Map.entry("debt_loans", List.of(
                    CreateCategoryCommand.builder()
                            .name("Debt & Loans")
                            .type(EXPENSE)
                            .icon("credit_card")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Installments & Amortization")
                            .type(EXPENSE)
                            .icon("receipt_long")
                            .build())),
            Map.entry("salary_active", List.of(CreateCategoryCommand.builder()
                    .name("Salary")
                    .type(INCOME)
                    .icon("payments")
                    .build())),
            Map.entry("business_sales", List.of(CreateCategoryCommand.builder()
                    .name("Business & Sales")
                    .type(INCOME)
                    .icon("storefront")
                    .build())),
            Map.entry("passive_investments", List.of(CreateCategoryCommand.builder()
                    .name("Investments")
                    .type(INCOME)
                    .icon("trending_up")
                    .build())),
            Map.entry("allowances_gifts", List.of(CreateCategoryCommand.builder()
                    .name("Gifts & Allowances")
                    .type(INCOME)
                    .icon("card_giftcard")
                    .build()))
    );
}
