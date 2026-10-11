package com.cinefy.service;

import com.cinefy.dto.*;
import com.cinefy.entity.User;
import com.cinefy.exception.ResourceNotFoundException;
import com.cinefy.repository.RatingRepository;
import com.cinefy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository users;
    private final RatingRepository ratings;
    private final RatingService ratingService;

    public UserService(UserRepository users, RatingRepository ratings, RatingService ratingService) {
        this.users = users;
        this.ratings = ratings;
        this.ratingService = ratingService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String q, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        String term = q == null ? "" : q.trim();
        Page<User> result = users.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                term, term, PageRequest.of(safePage, safeSize, Sort.by("name").ascending().and(Sort.by("id"))));
        Map<Long, double[]> stats = userStats();
        List<UserResponse> content = result.getContent().stream().map(u -> {
            double[] s = stats.getOrDefault(u.getId(), new double[]{0, 0});
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getCreatedAt(), (long) s[0], s[1]);
        }).toList();
        return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UserDetailResponse get(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        List<RatingResponse> history = ratings.findByUserWithDetails(id).stream()
                .map(ratingService::toResponse).toList();
        double avg = history.isEmpty() ? 0.0
                : Math.round(history.stream().mapToInt(RatingResponse::rating).average().orElse(0) * 100.0) / 100.0;
        return new UserDetailResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(),
                history.size(), avg, preferredGenres(id), history);
    }

    /** Genres ranked by the user's own average rating (then by how often they rated them). */
    @Transactional(readOnly = true)
    public List<GenrePreference> preferredGenres(Long userId) {
        return ratings.genreStatsForUser(userId).stream()
                .map(r -> new GenrePreference((String) r[0],
                        ((Number) r[2]).longValue(),
                        Math.round(((Number) r[1]).doubleValue() * 100.0) / 100.0))
                .sorted(Comparator.comparingDouble(GenrePreference::averageRating).reversed()
                        .thenComparing(Comparator.comparingLong(GenrePreference::ratingCount).reversed())
                        .thenComparing(GenrePreference::genre))
                .toList();
    }

    private Map<Long, double[]> userStats() {
        Map<Long, double[]> map = new HashMap<>();
        for (Object[] r : ratings.userStats()) {
            map.put(((Number) r[0]).longValue(),
                    new double[]{((Number) r[1]).doubleValue(), Math.round(((Number) r[2]).doubleValue() * 100.0) / 100.0});
        }
        return map;
    }
}
