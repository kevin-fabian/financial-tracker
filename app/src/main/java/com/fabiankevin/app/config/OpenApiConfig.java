package com.fabiankevin.app.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi accountsApi() {
        return GroupedOpenApi.builder()
                .group("accounts")
                .displayName("Accounts API")
                .pathsToMatch("/api/accounts/**")
                .build();
    }

    @Bean
    public GroupedOpenApi categoriesApi() {
        return GroupedOpenApi.builder()
                .group("categories")
                .displayName("Categories API")
                .pathsToMatch("/api/categories/**")
                .build();
    }

    @Bean
    public GroupedOpenApi transactionsApi() {
        return GroupedOpenApi.builder()
                .group("transactions")
                .displayName("Transactions API")
                .pathsToMatch("/api/transactions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statsApi() {
        return GroupedOpenApi.builder()
                .group("stats")
                .displayName("Stats API")
                .pathsToMatch("/api/stats")
                .pathsToMatch("/api/stats*")
                .build();
    }

    @Bean
    public GroupedOpenApi budgetsApi() {
        return GroupedOpenApi.builder()
                .group("budgets")
                .displayName("Budgets API")
                .pathsToMatch("/api/budgets/**")
                .build();
    }

    @Bean
    public GroupedOpenApi recurringTransactionsApi() {
        return GroupedOpenApi.builder()
                .group("recurring-transactions")
                .displayName("Recurring Transactions API")
                .pathsToMatch("/api/recurring-transactions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi householdsApi() {
        return GroupedOpenApi.builder()
                .group("households")
                .displayName("Households API")
                .pathsToMatch("/api/households/**")
                .build();
    }

    @Bean
    public GroupedOpenApi usersApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .displayName("Users API")
                .pathsToMatch("/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi shoppingListsApi() {
        return GroupedOpenApi.builder()
                .group("shopping-lists")
                .displayName("Shopping Lists API")
                .pathsToMatch("/api/shopping-lists/**")
                .build();
    }
}