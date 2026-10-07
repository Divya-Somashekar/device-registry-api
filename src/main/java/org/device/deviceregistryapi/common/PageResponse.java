package org.device.deviceregistryapi.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * A stable envelope for paged collections. Spring's {@code Page} is deliberately not
 * serialised directly, because its JSON form is not part of its public contract.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
