package com.cinefy.service;

import com.cinefy.dto.MovieRequest;
import com.cinefy.dto.MovieResponse;
import com.cinefy.dto.PageResponse;
import com.cinefy.entity.Movie;
import com.cinefy.exception.ResourceNotFoundException;
import com.cinefy.repository.MovieRepository;
import com.cinefy.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MovieService {

    private final MovieRepository movies;
    private final RatingRepository ratings;

    public MovieService(MovieRepository movies, RatingRepository ratings) {
        this.movies = movies;
        this.ratings = ratings;
    }

    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> list(String q, String genre, String sort, String dir, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        Map<Long, MovieStat> stats = stats();

        String sortKey = sort == null || sort.isBlank() ? "title" : sort;
        Comparator<MovieResponse> order = switch (sortKey) {
            case "year", "releaseYear" -> Comparator.comparing(MovieResponse::releaseYear);
            case "rating" -> Comparator.comparingDouble(MovieResponse::averageRating);
            case "popularity" -> Comparator.comparingLong(MovieResponse::ratingCount);
            default -> Comparator.comparing((MovieResponse m) -> m.title().toLowerCase(Locale.ROOT));
        };
        // Default direction: ascending for title, descending for year / rating / popularity.
        boolean descending = dir == null || dir.isBlank()
                ? !"title".equals(sortKey)
                : "desc".equalsIgnoreCase(dir);
        if (descending) {
            order = order.reversed();
        }
        order = order.thenComparing(m -> m.title().toLowerCase(Locale.ROOT));

        List<MovieResponse> all = movies.search(clean(q), clean(genre)).stream()
                .map(m -> toResponse(m, stats.getOrDefault(m.getId(), MovieStat.NONE)))
                .sorted(order)
                .toList();

        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return PageResponse.of(all.subList(from, to), safePage, safeSize, all.size());
    }

    @Transactional(readOnly = true)
    public MovieResponse get(Long id) {
        Movie movie = find(id);
        return toResponse(movie, stats().getOrDefault(id, MovieStat.NONE));
    }

    @Transactional(readOnly = true)
    public List<String> genres() {
        return movies.findGenres();
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> related(Long id, int limit) {
        Movie movie = find(id);
        Map<Long, MovieStat> stats = stats();
        return movies.findByGenreAndIdNot(movie.getGenre(), id).stream()
                .map(m -> toResponse(m, stats.getOrDefault(m.getId(), MovieStat.NONE)))
                .sorted(Comparator.comparingDouble(MovieResponse::averageRating).reversed()
                        .thenComparing(Comparator.comparingLong(MovieResponse::ratingCount).reversed()))
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        return toResponse(movies.save(movie), MovieStat.NONE);
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = find(id);
        apply(movie, request);
        Movie saved = movies.save(movie);
        return toResponse(saved, stats().getOrDefault(id, MovieStat.NONE));
    }

    @Transactional
    public void delete(Long id) {
        Movie movie = find(id);
        ratings.deleteByMovieId(id);
        movies.delete(movie);
    }

    // ---------- shared helpers used by other services ----------

    /** Average rating and rating count for every rated movie, keyed by movie id. */
    @Transactional(readOnly = true)
    public Map<Long, MovieStat> stats() {
        Map<Long, MovieStat> map = new HashMap<>();
        for (Object[] row : ratings.movieStats()) {
            map.put(((Number) row[0]).longValue(),
                    new MovieStat(round2(((Number) row[1]).doubleValue()), ((Number) row[2]).longValue()));
        }
        return map;
    }

    public MovieResponse toResponse(Movie m, MovieStat s) {
        return new MovieResponse(m.getId(), m.getTitle(), m.getDescription(), m.getGenre(), m.getReleaseYear(),
                m.getPosterUrl(), s.average(), s.count(), m.getCreatedAt());
    }

    private Movie find(Long id) {
        return movies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    private void apply(Movie movie, MovieRequest r) {
        movie.setTitle(r.title().trim());
        movie.setDescription(blankToNull(r.description()));
        movie.setGenre(r.genre().trim());
        movie.setReleaseYear(r.releaseYear());
        movie.setPosterUrl(blankToNull(r.posterUrl()));
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
