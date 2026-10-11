package com.cinefy.service.recommendation;

/** How a user has rated one genre so far. */
public record GenreStat(String genre, long count, double averageRating) {
}
