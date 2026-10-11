package com.cinefy.dto;

import java.util.List;

public record RecommendationResponse(Long userId, String userName, boolean coldStart,
                                     List<GenrePreference> topGenres, List<RecommendedMovie> recommendations) {
}
