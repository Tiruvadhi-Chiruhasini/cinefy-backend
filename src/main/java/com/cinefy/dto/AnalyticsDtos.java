package com.cinefy.dto;

import java.util.List;

/** Response shapes for the analytics endpoints. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record Overview(long totalMovies, long totalUsers, long totalRatings, double averageRating,
                           List<MovieResponse> recentMovies) {
    }

    public record GenreCount(String genre, long movieCount, long ratingCount, double averageRating) {
    }

    public record RatingBucket(int rating, long count) {
    }

    public record TrendPoint(String month, long count, double averageRating) {
    }

    public record RatingAnalytics(long totalRatings, double averageRating,
                                  List<RatingBucket> distribution, List<TrendPoint> trend) {
    }
}
