package com.cinefy.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserDetailResponse(Long id, String name, String email, LocalDateTime createdAt,
                                 long ratingCount, double averageRating,
                                 List<GenrePreference> preferredGenres, List<RatingResponse> ratings) {
}
