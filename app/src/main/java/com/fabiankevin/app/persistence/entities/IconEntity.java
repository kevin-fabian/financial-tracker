package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.IconData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "icons",
        indexes = {
                @Index(name = "idx_icons_code_point", columnList = "code_point")
        })
@Entity
public class IconEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_point", nullable = false)
    private int codePoint;

    @Column(name = "font_family", nullable = false, length = 128)
    private String fontFamily;

    @Column(name = "icon_name", nullable = false, length = 128)
    private String iconName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static IconEntity from(IconData iconData) {
        if (iconData == null) return null;
        return IconEntity.builder()
                .id(iconData.id())
                .codePoint(iconData.codePoint())
                .fontFamily(iconData.fontFamily())
                .iconName(iconData.iconName())
                .createdAt(iconData.createdAt())
                .build();
    }

    public IconData toModel() {
        return IconData.builder()
                .id(this.id)
                .codePoint(this.codePoint)
                .fontFamily(this.fontFamily)
                .iconName(this.iconName)
                .createdAt(this.createdAt)
                .build();
    }
}
