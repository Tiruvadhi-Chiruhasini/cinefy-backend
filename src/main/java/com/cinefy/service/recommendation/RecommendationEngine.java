package com.cinefy.service.recommendation;

import java.util.List;

/**
 * Strategy interface. The current implementation is content-based (genre preference + community rating).
 * A collaborative-filtering or ML engine can be added later by implementing this interface.
 */
public interface RecommendationEngine {

    /** Ranks the genres a user has rated, strongest preference first. */
    List<GenreAffinity> rankGenres(List<GenreStat> userGenres);

    /**
     * @param userGenres the user's rating history grouped by genre (empty for a new user)
     * @param candidates movies the user has NOT rated
     * @param globalMean average of all ratings in the system (used as a neutral prior)
     * @param limit      maximum number of results
     */
    List<ScoredMovie> recommend(List<GenreStat> userGenres, List<Candidate> candidates, double globalMean, int limit);
}
