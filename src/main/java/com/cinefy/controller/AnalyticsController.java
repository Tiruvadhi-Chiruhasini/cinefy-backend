package com.cinefy.controller;

import com.cinefy.dto.AnalyticsDtos.*;
import com.cinefy.dto.MovieResponse;
import com.cinefy.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return service.overview();
    }

    @GetMapping("/genres")
    public List<GenreCount> genres() {
        return service.genres();
    }

    /** Rating distribution and monthly trend, optionally for a single genre. */
    @GetMapping("/ratings")
    public RatingAnalytics ratings(@RequestParam(defaultValue = "") String genre) {
        return service.ratingAnalytics(genre);
    }

    @GetMapping("/top-movies")
    public List<MovieResponse> topMovies(@RequestParam(defaultValue = "10") int limit,
                                         @RequestParam(defaultValue = "3") int minRatings,
                                         @RequestParam(defaultValue = "") String genre) {
        return service.topMovies(limit, minRatings, genre);
    }

    @GetMapping("/popular-movies")
    public List<MovieResponse> popularMovies(@RequestParam(defaultValue = "10") int limit,
                                             @RequestParam(defaultValue = "") String genre) {
        return service.popularMovies(limit, genre);
    }
}
