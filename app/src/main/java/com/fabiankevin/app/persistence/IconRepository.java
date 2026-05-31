package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.IconData;

import java.util.Optional;

public interface IconRepository {
    Optional<IconData> findByCodePointAndFontFamily(int codePoint, String fontFamily);
}
