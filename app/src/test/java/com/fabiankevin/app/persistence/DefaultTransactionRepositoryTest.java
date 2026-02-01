package com.fabiankevin.app.persistence;

import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
class DefaultTransactionRepositoryTest {
    @Autowired
    private JpaAccountRepository jpaAccountRepository;
    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;
    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;
}