package com.cinefy.service.recommendation;

/** A genre plus how strongly the user prefers it (0 to 1). */
public record GenreAffinity(String genre, long count, double averageRating, double affinity) {
}
