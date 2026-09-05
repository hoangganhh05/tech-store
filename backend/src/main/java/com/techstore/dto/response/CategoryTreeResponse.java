package com.techstore.dto.response;

import java.util.List;

public record CategoryTreeResponse(
        Long id,
        String name,
        String description,
        Long parentId,
        String imageUrl,
        List<CategoryTreeResponse> children
) {
}