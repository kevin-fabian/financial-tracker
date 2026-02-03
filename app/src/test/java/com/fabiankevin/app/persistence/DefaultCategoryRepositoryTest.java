package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
class DefaultCategoryRepositoryTest {

    @MockitoSpyBean
    private JpaCategoryRepository jpaCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public CategoryRepository categoryRepository(JpaCategoryRepository jpaCategoryRepository) {
            return new DefaultCategoryRepository(jpaCategoryRepository);
        }
    }

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .name("FOOD")
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenValidCategory_shouldPersistAndRetrieve() {
        Category saved = categoryRepository.save(category);

        var found = categoryRepository.findByIdAndUserId(saved.id(), saved.userId()).orElseThrow();

        Assertions.assertThat(found)
                .as("found category should match saved category ignoring id")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);

        verify(jpaCategoryRepository, times(1)).save(any());
        verify(jpaCategoryRepository, times(1)).findByIdAndUserId(saved.id(), saved.userId());
    }

    @Test
    void save_givenNull_shouldThrowIllegalArgumentException() {
        Assertions.assertThatThrownBy(() -> categoryRepository.save(null))
                .as("saving null should throw InvalidDataAccessApiUsageException")
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    void deleteById_givenExistingCategory_shouldRemoveCategory() {
        Category saved = categoryRepository.save(category);

        categoryRepository.deleteByIdAndUserId(saved.id(), saved.userId());

        Optional<Category> found = categoryRepository.findByIdAndUserId(saved.id(), saved.userId());
        Assertions.assertThat(found).as("category should be deleted and retrieval should return empty optional").isEmpty();

        verify(jpaCategoryRepository, times(1)).deleteByIdAndUserId(saved.id(), saved.userId());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var found = categoryRepository.findByIdAndUserId(id, userId);

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }


    @Test
    void findById_givenExisting_shouldReturnCategory() {
        Category saved = categoryRepository.save(category);
        Category found = categoryRepository.findById(saved.id()).get();

        Assertions.assertThat(found).as("non existing id returns empty optional")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);
    }
}
