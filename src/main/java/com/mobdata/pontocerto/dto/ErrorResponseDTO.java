package com.mobdata.pontocerto.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDTO(
        String message,
        String code,       // ex: "TOKEN_MISSING", "TOKEN_EXPIRED", "TOKEN_INVALID"
        int status,
        LocalDateTime timestamp,
        Map<String, String> fields
) {
}
