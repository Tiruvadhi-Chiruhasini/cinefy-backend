package com.cinefy.service.recommendation;

/** A recommended movie with its 0-1 score and a human-readable explanation. */
public record ScoredMovie(long movieId, double score, String reason) {
}
