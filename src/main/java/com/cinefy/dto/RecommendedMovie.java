package com.cinefy.dto;

public record RecommendedMovie(MovieResponse movie, double score, String reason) {
}
