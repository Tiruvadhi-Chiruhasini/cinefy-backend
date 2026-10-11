package com.cinefy.service.recommendation;

/** A movie the user has not rated yet, with its community rating statistics. */
public record Candidate(long movieId, String genre, double averageRating, long ratingCount) {
}
