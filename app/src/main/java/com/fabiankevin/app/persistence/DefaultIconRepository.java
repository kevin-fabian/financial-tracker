package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.persistence.entities.IconEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaIconRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class DefaultIconRepository implements IconRepository {
    private final JpaIconRepository jpaIconRepository;

    @Override
    public Optional<IconData> findByCodePointAndFontFamily(int codePoint, String fontFamily) {
        return jpaIconRepository.findByCodePointAndFontFamily(codePoint, fontFamily)
                .map(IconEntity::toModel);
    }

    @Override
    public IconData save(IconData iconData) {
        return jpaIconRepository.save(IconEntity.from(iconData)).toModel();
    }
}
