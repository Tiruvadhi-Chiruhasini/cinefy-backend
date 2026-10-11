package com.cinefy.service;

import com.cinefy.dto.AnalyticsDtos.*;
import com.cinefy.dto.MovieResponse;
import com.cinefy.entity.Movie;
import com.cinefy.repository.MovieRepository;
import com.cinefy.repository.RatingRepository;
import com.cinefy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final MovieRepository movies;
    private final UserRepository users;
    private final RatingRepository ratings;
    private final MovieService movieService;

    public AnalyticsService(MovieRepository movies, UserRepository users, RatingRepository ratings,
                            MovieService movieService) {
        this.movies = movies;
        this.users = users;
        this.ratings = ratings;
        this.movieService = movieService;
    }

    public Overview overview() {
        Map<Long, MovieStat> stats = movieService.stats();
        List<MovieResponse> recent = movies.findTop8ByOrderByCreatedAtDescIdDesc().stream()
                .map(m -> movieService.toResponse(m, stats.getOrDefault(m.getId(), MovieStat.NONE)))
                .toList();
        double avg = ratings.globalAverage() == null ? 0.0 : ratings.globalAverage();
        return new Overview(movies.count(), users.count(), ratings.count(), MovieService.round2(avg), recent);
    }

    public List<GenreCount> genres() {
        Map<String, double[]> ratingStats = new HashMap<>();
        for (Object[] r : ratings.genreStats()) {
            ratingStats.put((String) r[0],
                    new double[]{((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue()});
        }
        List<GenreCount> result = new ArrayList<>();
        for (Object[] r : movies.countByGenre()) {
            String genre = (String) r[0];
            double[] rs = ratingStats.getOrDefault(genre, new double[]{0, 0});
            result.add(new GenreCount(genre, ((Number) r[1]).longValue(), (long) rs[1], MovieService.round2(rs[0])));
        }
        return result;
    }

    public RatingAnalytics ratingAnalytics(String genre) {
        List<Object[]> points = ratings.ratingPoints(genre == null ? "" : genre.trim());
        long[] buckets = new long[6];
        TreeMap<YearMonth, double[]> months = new TreeMap<>();
        double sum = 0;
        for (Object[] p : points) {
            int value = ((Number) p[0]).intValue();
            buckets[value]++;
            sum += value;
            YearMonth ym = YearMonth.from((LocalDateTime) p[1]);
            double[] agg = months.computeIfAbsent(ym, k -> new double[2]);
            agg[0]++;
            agg[1] += value;
        }
        List<RatingBucket> distribution = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            distribution.add(new RatingBucket(i, buckets[i]));
        }
        List<TrendPoint> trend = new ArrayList<>();
        months.forEach((ym, agg) -> trend.add(
                new TrendPoint(ym.toString(), (long) agg[0], MovieService.round2(agg[1] / agg[0]))));
        double avg = points.isEmpty() ? 0.0 : MovieService.round2(sum / points.size());
        return new RatingAnalytics(points.size(), avg, distribution, trend);
    }

    /** Highest average rating; movies need at least minRatings ratings so one lucky vote cannot win. */
    public List<MovieResponse> topMovies(int limit, int minRatings, String genre) {
        int min = Math.max(1, minRatings);
        return rankedMovies(genre).stream()
                .filter(m -> m.ratingCount() >= min)
                .sorted(Comparator.comparingDouble(MovieResponse::averageRating).reversed()
                        .thenComparing(Comparator.comparingLong(MovieResponse::ratingCount).reversed())
                        .thenComparing(MovieResponse::title))
                .limit(clampLimit(limit))
                .toList();
    }

    /** Most-rated movies. */
    public List<MovieResponse> popularMovies(int limit, String genre) {
        return rankedMovies(genre).stream()
                .filter(m -> m.ratingCount() > 0)
                .sorted(Comparator.comparingLong(MovieResponse::ratingCount).reversed()
                        .thenComparing(Comparator.comparingDouble(MovieResponse::averageRating).reversed())
                        .thenComparing(MovieResponse::title))
                .limit(clampLimit(limit))
                .toList();
    }

    private List<MovieResponse> rankedMovies(String genre) {
        Map<Long, MovieStat> stats = movieService.stats();
        List<Movie> source = movies.search("", genre == null ? "" : genre.trim());
        return source.stream()
                .map(m -> movieService.toResponse(m, stats.getOrDefault(m.getId(), MovieStat.NONE)))
                .toList();
    }

    private static int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }
}
