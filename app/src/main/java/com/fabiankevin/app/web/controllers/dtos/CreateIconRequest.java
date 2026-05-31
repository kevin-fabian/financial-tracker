package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.IconData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating an icon")
public record CreateIconRequest(
        @NotNull(message = "Code point is required")
        @Schema(description = "Unicode code point of the icon", example = "128161")
        int codePoint,
        @NotNull(message = "Font family is required")
        @Schema(description = "Font family of the icon", example = "Material Icons")
        String fontFamily
) {
    public IconData toIconData() {
        return IconData.builder()
                .codePoint(this.codePoint)
                .fontFamily(this.fontFamily)
                .build();
    }
}
