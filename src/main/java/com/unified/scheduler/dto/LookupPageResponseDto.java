package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response designed for dropdown infinite-scrolling / typeahead components")
public record LookupPageResponseDto(
    @Schema(description = "Page of lightweight option items")
    List<LookupOptionDto> items,

    @Schema(description = "Current page index (0-based)", example = "0")
    int page,

    @Schema(description = "Requested page size limit", example = "20")
    int limit,

    @Schema(description = "Total number of matching elements across all pages", example = "156")
    long totalElements,

    @Schema(description = "True if subsequent items exist to trigger further infinite-scroll requests", example = "true")
    boolean hasMore
) {
    public static LookupPageResponseDto of(List<LookupOptionDto> items, int page, int limit, long totalElements) {
        boolean hasMore = (long) (page + 1) * limit < totalElements;
        return new LookupPageResponseDto(items, page, limit, totalElements, hasMore);
    }
}
