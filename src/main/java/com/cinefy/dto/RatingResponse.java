package com.cinefy.dto;

import java.time.LocalDateTime;

public record RatingResponse(Long id, Long userId, String userName, Long movieId, String movieTitle,
                             String movieGenre, int rating, LocalDateTime createdAt) {
}
