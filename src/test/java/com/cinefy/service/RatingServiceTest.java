package com.cinefy.service;

import com.cinefy.dto.RatingRequest;
import com.cinefy.dto.RatingResponse;
import com.cinefy.entity.Movie;
import com.cinefy.entity.Rating;
import com.cinefy.entity.User;
import com.cinefy.exception.DuplicateRatingException;
import com.cinefy.exception.ResourceNotFoundException;
import com.cinefy.repository.MovieRepository;
import com.cinefy.repository.RatingRepository;
import com.cinefy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RatingServiceTest {

    @Mock RatingRepository ratings;
    @Mock UserRepository users;
    @Mock MovieRepository movies;

    RatingService service;
    User user;
    Movie movie;

    @BeforeEach
    void setUp() {
        service = new RatingService(ratings, users, movies);
        user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setName("Test User");
        movie = new Movie();
        ReflectionTestUtils.setField(movie, "id", 2L);
        movie.setTitle("Test Movie");
        movie.setGenre("Drama");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(movies.findById(2L)).thenReturn(Optional.of(movie));
    }

    @Test
    void createSavesRatingForNewUserMoviePair() {
        when(ratings.existsByUserIdAndMovieId(1L, 2L)).thenReturn(false);
        when(ratings.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

        RatingResponse response = service.create(new RatingRequest(1L, 2L, 4));

        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.movieTitle()).isEqualTo("Test Movie");
    }

    @Test
    void createRejectsSecondRatingForSameMovie() {
        when(ratings.existsByUserIdAndMovieId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new RatingRequest(1L, 2L, 5)))
                .isInstanceOf(DuplicateRatingException.class);
        verify(ratings, never()).save(any());
    }

    @Test
    void createFailsForUnknownMovie() {
        when(movies.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new RatingRequest(1L, 99L, 3)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Movie");
    }
}
