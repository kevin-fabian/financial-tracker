package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.embeddables.AmountEmbeddable;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

@ActiveProfiles("test")
@DataJpaTest
class DefaultTransactionRepositoryTest {
    @Autowired
    private JpaAccountRepository jpaAccountRepository;
    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;
    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;


    @Test
    void save_givenValidTransaction_shouldSave() {
        CategoryEntity category = new CategoryEntity();
        category.setName("FOOD");
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        CategoryEntity savedCategory = jpaCategoryRepository.save(category);

        AccountEntity account = new AccountEntity();
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        account.setCurrency("PHP");
        account.setName("GCASH");
        AccountEntity savedAccount = jpaAccountRepository.save(account);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setCategory(savedCategory);
        transaction.setAccount(savedAccount);
        transaction.setDescription("FOOD");
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(AmountEmbeddable.builder()
                        .amount(BigDecimal.valueOf(100))
                        .currency("PHP")
                .build());
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        jpaTransactionRepository.saveAndFlush(transaction);
    }
}