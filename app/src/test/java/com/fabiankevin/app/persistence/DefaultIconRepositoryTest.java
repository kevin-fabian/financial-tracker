package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.persistence.entities.IconEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaIconRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
class DefaultIconRepositoryTest {

    @MockitoSpyBean
    private JpaIconRepository jpaIconRepository;

    @Autowired
    private IconRepository iconRepository;

    private IconData icon;

    @org.springframework.boot.test.context.TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public IconRepository iconRepository(JpaIconRepository jpaIconRepository) {
            return new DefaultIconRepository(jpaIconRepository);
        }
    }

    @BeforeEach
    void setUp() {
        icon = IconData.builder()
                .codePoint(0x1F4B0)
                .fontFamily("MaterialIcons")
                .iconName("attach_money")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void findByCodePointAndFontFamily_existingIcon_shouldReturnIcon() {
        jpaIconRepository.save(IconEntity.from(icon));

        var found = iconRepository.findByCodePointAndFontFamily(
                icon.codePoint(), icon.fontFamily());

        assertThat(found).isPresent();
        assertThat(found.get().codePoint()).isEqualTo(icon.codePoint());
        assertThat(found.get().fontFamily()).isEqualTo(icon.fontFamily());
        assertThat(found.get().iconName()).isEqualTo(icon.iconName());

        verify(jpaIconRepository, times(1)).findByCodePointAndFontFamily(
                icon.codePoint(), icon.fontFamily());
    }

    @Test
    void findByCodePointAndFontFamily_nonExisting_shouldReturnEmpty() {
        var found = iconRepository.findByCodePointAndFontFamily(
                9999, "NonExistent");

        assertThat(found).isEmpty();

        verify(jpaIconRepository, times(1)).findByCodePointAndFontFamily(
                9999, "NonExistent");
    }
}
