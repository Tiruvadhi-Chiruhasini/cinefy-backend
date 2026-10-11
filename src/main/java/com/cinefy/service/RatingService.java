package com.cinefy.service;

import com.cinefy.dto.PageResponse;
import com.cinefy.dto.RatingRequest;
import com.cinefy.dto.RatingResponse;
import com.cinefy.dto.RatingUpdateRequest;
import com.cinefy.entity.Movie;
import com.cinefy.entity.Rating;
import com.cinefy.entity.User;
import com.cinefy.exception.DuplicateRatingException;
import com.cinefy.exception.ResourceNotFoundException;
import com.cinefy.repository.MovieRepository;
import com.cinefy.repository.RatingRepository;
import com.cinefy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratings;
    private final UserRepository users;
    private final MovieRepository movies;

    public RatingService(RatingRepository ratings, UserRepository users, MovieRepository movies) {
        this.ratings = ratings;
        this.users = users;
        this.movies = movies;
    }

    @Transactional(readOnly = true)
    public PageResponse<RatingResponse> list(Long userId, Long movieId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        Page<Rating> result = ratings.search(userId == null ? 0L : userId, movieId == null ? 0L : movieId,
                PageRequest.of(safePage, safeSize));
        return PageResponse.of(result.getContent().stream().map(this::toResponse).toList(),
                safePage, safeSize, result.getTotalElements());
    }

    @Transactional
    public RatingResponse create(RatingRequest request) {
        User user = users.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));
        Movie movie = movies.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.movieId()));
        if (ratings.existsByUserIdAndMovieId(user.getId(), movie.getId())) {
            throw new DuplicateRatingException();
        }
        Rating rating = new Rating();
        rating.setUser(user);
        rating.setMovie(movie);
        rating.setRating(request.rating());
        return toResponse(ratings.save(rating));
    }

    @Transactional
    public RatingResponse update(Long id, RatingUpdateRequest request) {
        Rating rating = ratings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rating", id));
        rating.setRating(request.rating());
        return toResponse(ratings.save(rating));
    }

    @Transactional
    public void delete(Long id) {
        Rating rating = ratings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rating", id));
        ratings.delete(rating);
    }

    public RatingResponse toResponse(Rating r) {
        return new RatingResponse(r.getId(), r.getUser().getId(), r.getUser().getName(),
                r.getMovie().getId(), r.getMovie().getTitle(), r.getMovie().getGenre(),
                r.getRating(), r.getCreatedAt());
    }
}
