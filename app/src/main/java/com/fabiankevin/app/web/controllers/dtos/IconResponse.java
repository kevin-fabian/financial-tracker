package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.IconData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing an icon")
public record IconResponse(
        @Schema(description = "Unique identifier of the icon", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Unicode code point of the icon", example = "128161")
        int codePoint,
        @Schema(description = "Font family of the icon", example = "Material Icons")
        String fontFamily
) {
    public static IconResponse from(final IconData iconData) {
        if (iconData == null) return null;
        return IconResponse.builder()
                .id(iconData.id())
                .codePoint(iconData.codePoint())
                .fontFamily(iconData.fontFamily())
                .build();
    }
}
