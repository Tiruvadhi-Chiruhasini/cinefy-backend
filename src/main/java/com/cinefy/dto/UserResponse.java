package com.cinefy.dto;

import java.time.LocalDateTime;

/** Public view of a user. Never contains passwords or other sensitive data. */
public record UserResponse(Long id, String name, String email, LocalDateTime createdAt,
                           long ratingCount, double averageRating) {
}
