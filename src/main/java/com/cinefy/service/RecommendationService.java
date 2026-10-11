package com.cinefy.service;

import com.cinefy.dto.GenrePreference;
import com.cinefy.dto.MovieResponse;
import com.cinefy.dto.RecommendationResponse;
import com.cinefy.dto.RecommendedMovie;
import com.cinefy.entity.Movie;
import com.cinefy.entity.User;
import com.cinefy.exception.ResourceNotFoundException;
import com.cinefy.repository.MovieRepository;
import com.cinefy.repository.RatingRepository;
import com.cinefy.repository.UserRepository;
import com.cinefy.service.recommendation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads data from the database and delegates scoring to a {@link RecommendationEngine}.
 * To try a different algorithm, provide another RecommendationEngine bean.
 */
@Service
public class RecommendationService {

    private final UserRepository users;
    private final MovieRepository movies;
    private final RatingRepository ratings;
    private final MovieService movieService;
    private final RecommendationEngine engine;

    public RecommendationService(UserRepository users, MovieRepository movies, RatingRepository ratings,
                                 MovieService movieService, RecommendationEngine engine) {
        this.users = users;
        this.movies = movies;
        this.ratings = ratings;
        this.movieService = movieService;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(Long userId, int limit) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<GenreStat> genreStats = ratings.genreStatsForUser(userId).stream()
                .map(r -> new GenreStat((String) r[0], ((Number) r[2]).longValue(), ((Number) r[1]).doubleValue()))
                .toList();
        Set<Long> rated = ratings.findRatedMovieIds(userId);
        Map<Long, MovieStat> stats = movieService.stats();
        double globalMean = ratings.globalAverage() == null ? 0.0 : ratings.globalAverage();

        Map<Long, Movie> unrated = new HashMap<>();
        List<Candidate> candidates = movies.findAll().stream()
                .filter(m -> !rated.contains(m.getId()))
                .peek(m -> unrated.put(m.getId(), m))
                .map(m -> {
                    MovieStat s = stats.getOrDefault(m.getId(), MovieStat.NONE);
                    return new Candidate(m.getId(), m.getGenre(), s.average(), s.count());
                })
                .toList();

        List<RecommendedMovie> recommendations = engine.recommend(genreStats, candidates, globalMean, safeLimit)
                .stream()
                .map(sm -> {
                    Movie m = unrated.get(sm.movieId());
                    MovieResponse mr = movieService.toResponse(m, stats.getOrDefault(m.getId(), MovieStat.NONE));
                    return new RecommendedMovie(mr, sm.score(), sm.reason());
                })
                .toList();

        List<GenrePreference> topGenres = engine.rankGenres(genreStats).stream().limit(3)
                .map(g -> new GenrePreference(g.genre(), g.count(), Math.round(g.averageRating() * 100.0) / 100.0))
                .toList();

        return new RecommendationResponse(user.getId(), user.getName(), genreStats.isEmpty(), topGenres, recommendations);
    }
}
