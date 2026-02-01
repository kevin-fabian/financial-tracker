package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
}
