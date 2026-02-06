package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
class DefaultAccountRepositoryTest {

    @MockitoSpyBean
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account account;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public AccountRepository accountRepository(JpaAccountRepository jpaAccountRepository) {
            return new DefaultAccountRepository(jpaAccountRepository);
        }
    }

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .name("GCASH")
                .userId(UUID.randomUUID())
                .currency(java.util.Currency.getInstance("PHP"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenValidAccount_shouldPersistAndRetrieve() {
        Account saved = accountRepository.save(account);

        var found = accountRepository.findById(saved.id()).orElseThrow();

        Assertions.assertThat(found)
                .as("found account should match saved account ignoring id")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);

        verify(jpaAccountRepository, times(1)).save(any());
        verify(jpaAccountRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenExistingAccount_shouldReturnAccount() {
        Account saved = accountRepository.save(account);

        var found = accountRepository.findById(saved.id()).orElseThrow();

        Assertions.assertThat(found)
                .as("found account should match saved account ignoring id")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);

        verify(jpaAccountRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        var found = accountRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Test
    void deleteById_givenExistingAccount_shouldRemoveAccount() {
        Account saved = accountRepository.save(account);

        accountRepository.deleteById(saved.id());

        Optional<Account> found = accountRepository.findById(saved.id());
        Assertions.assertThat(found).as("account should be deleted and retrieval should return empty optional").isEmpty();

        verify(jpaAccountRepository, times(1)).deleteById(saved.id());
    }

    @Test
    void getAccountsByPageAndUserId_givenMultipleAccounts_thenShouldReturnPagedResults() {
        UUID userId = UUID.randomUUID();

        // create and save 5 accounts for the same user
        for (int i = 0; i < 5; i++) {
            Account a = Account.builder()
                    .name("Account " + i)
                    .userId(userId)
                    .currency(java.util.Currency.getInstance("PHP"))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            accountRepository.save(a);
        }

        PageQuery query = new PageQuery(0, 3, "name", "ASC");
        Page<Account> page = accountRepository.getAccountsByPageAndUserId(query, userId);

        Assertions.assertThat(page.content()).as("page should contain 3 elements").hasSize(3);
        Assertions.assertThat(page.totalElements()).isEqualTo(5);
        Assertions.assertThat(page.page()).isZero();
        Assertions.assertThat(page.size()).isEqualTo(3);

        verify(jpaAccountRepository, times(1)).findAllByUserId(userId, PageRequest.of(0, 3, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.fromString("ASC"), "name")));
    }
}
