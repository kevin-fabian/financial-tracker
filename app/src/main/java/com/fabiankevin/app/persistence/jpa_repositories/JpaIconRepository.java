package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.IconEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaIconRepository extends JpaRepository<IconEntity, UUID> {
    Optional<IconEntity> findByCodePointAndFontFamily(int codePoint, String fontFamily);
}
