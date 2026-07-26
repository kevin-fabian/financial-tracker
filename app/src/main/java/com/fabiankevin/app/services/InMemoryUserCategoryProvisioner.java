package com.fabiankevin.app.services;

import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static com.fabiankevin.app.models.enums.TransactionType.INCOME;

@Component
@RequiredArgsConstructor
public class InMemoryUserCategoryProvisioner implements UserCategoryProvisioner {
    private final CategoryService categoryService;

    @Transactional
    @Override
    public void provision(Set<String> categoryInterests, UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Set<String> categoryInterestWithDefault = new HashSet<>(Optional.ofNullable(categoryInterests).orElse(Set.of()));
        categoryInterestWithDefault.add("default");

        categoryService.deleteAllByUserId(userId);
        categoryInterestWithDefault.stream()
                .filter(CATEGORY_INTERESTS_MAPPING::containsKey)
                .flatMap(interest -> CATEGORY_INTERESTS_MAPPING.get(interest).stream())
                .forEach(command -> categoryService.createCategory(command.toBuilder().userId(userId).build()));
    }

    private static final Map<String, List<CreateCategoryCommand>> CATEGORY_INTERESTS_MAPPING = Map.ofEntries(
            // === UNIVERSAL BASICS (Applied to all users) ===
            Map.entry("default", List.of(
                    CreateCategoryCommand.builder()
                            .name("Food & Dining")
                            .type(EXPENSE)
                            .icon("restaurant")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Transportation")
                            .type(EXPENSE)
                            .icon("directions_bus")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Side Hustle")
                            .type(INCOME)
                            .icon("attach_money")
                            .build()
            )),

            // === Essential Housing & Living Expenses ===
            Map.entry("groceries", List.of(
                    CreateCategoryCommand.builder()
                            .name("Groceries")
                            .type(EXPENSE)
                            .icon("local_grocery_store")
                            .build()
            )),
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
                            .build()
            )),

            Map.entry("bills", List.of(
                    CreateCategoryCommand.builder()
                            .name("Utilities & Bills")
                            .type(EXPENSE)
                            .icon("receipt_long")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Subscriptions")
                            .type(EXPENSE)
                            .icon("card_membership")
                            .build()
            )),

            // === LIFESTYLE & PERSONAL ===
            Map.entry("entertainment", List.of(
                    CreateCategoryCommand.builder()
                            .name("Entertainment & Hobbies")
                            .type(EXPENSE)
                            .icon("sports_esports")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Travel & Vacation")
                            .type(EXPENSE)
                            .icon("flight")
                            .build()
            )),
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
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Clothing & Footwear")
                            .type(EXPENSE)
                            .icon("checkroom")
                            .build()
            )),

            // === HEALTH & INSURANCE ===
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
                            .build()
            )),
            Map.entry("insurance", List.of(  // [NEW GROUP] Critical protective expenses
                    CreateCategoryCommand.builder()
                            .name("Health Insurance")
                            .type(EXPENSE)
                            .icon("health_and_safety")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Auto Insurance")
                            .type(EXPENSE)
                            .icon("car_repair")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Life Insurance")
                            .type(EXPENSE)
                            .icon("family_restroom")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Disability Insurance")
                            .type(EXPENSE)
                            .icon("accessible")
                            .build()
            )),

            // === FAMILY & DEPENDENTS ===
            Map.entry("family_pets", List.of(
                    CreateCategoryCommand.builder()
                            .name("Family & Kids")
                            .type(EXPENSE)
                            .icon("family_restroom")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Childcare")
                            .type(EXPENSE)
                            .icon("child_care")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Pets")
                            .type(EXPENSE)
                            .icon("pets")
                            .build()
            )),

            // === FINANCIAL MANAGEMENT ===
            Map.entry("savings", List.of(
                    CreateCategoryCommand.builder()
                            .name("Savings & Goals")
                            .type(EXPENSE)
                            .icon("savings")
                            .build()
            )),
            Map.entry("debt_loans", List.of(
                    CreateCategoryCommand.builder()
                            .name("Debt & Transfers")
                            .type(EXPENSE)
                            .icon("swap_horiz")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Installments & Amortization")
                            .type(EXPENSE)
                            .icon("receipt_long")
                            .build()
            )),
            Map.entry("education", List.of(
                    CreateCategoryCommand.builder()
                            .name("Tuition & School Fees")
                            .type(EXPENSE)
                            .icon("account_balance_wallet")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Books & Supplies")
                            .type(EXPENSE)
                            .icon("book")
                            .build()
            )),

            // === GIVING & TAXES ===
            Map.entry("giving", List.of(
                    CreateCategoryCommand.builder()
                            .name("Gifts & Occasions")  // [NEW] Birthday, holiday giving
                            .type(EXPENSE)
                            .icon("card_giftcard")
                            .build()
            )),

            // === VEHICLE EXPENSES ===
            Map.entry("vehicle", List.of(  // [NEW GROUP] For car owners
                    CreateCategoryCommand.builder()
                            .name("Fuel & Gas")
                            .type(EXPENSE)
                            .icon("local_gas_station")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Vehicle Maintenance")
                            .type(EXPENSE)
                            .icon("car_repair")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Parking & Tolls")
                            .type(EXPENSE)
                            .icon("local_parking")
                            .build()
            )),

            // === INCOME CATEGORIES ===
            Map.entry("salary_active", List.of(
                    CreateCategoryCommand.builder()
                            .name("Salary & Wage")
                            .type(INCOME)
                            .icon("payments")
                            .build()
            )),
            Map.entry("business_sales", List.of(
                    CreateCategoryCommand.builder()
                            .name("Business & Sales")
                            .type(INCOME)
                            .icon("storefront")
                            .build()
            )),
            Map.entry("passive_investments", List.of(
                    CreateCategoryCommand.builder()
                            .name("Investments")
                            .type(INCOME)
                            .icon("trending_up")
                            .build()
            )),
            Map.entry("allowances_gifts", List.of(
                    CreateCategoryCommand.builder()
                            .name("Gifts & Allowances")  // Income gifts
                            .type(INCOME)
                            .icon("card_giftcard")
                            .build(),
                    CreateCategoryCommand.builder()
                            .name("Government Benefits")  // [NEW] Social security, unemployment, etc.
                            .type(INCOME)
                            .icon("account_balance")
                            .build()
            ))
    );
}
