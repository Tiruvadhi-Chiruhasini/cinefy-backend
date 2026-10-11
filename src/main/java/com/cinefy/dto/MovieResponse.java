package com.cinefy.dto;

import java.time.LocalDateTime;

public record MovieResponse(Long id, String title, String description, String genre, Integer releaseYear,
                            String posterUrl, double averageRating, long ratingCount, LocalDateTime createdAt) {
}
