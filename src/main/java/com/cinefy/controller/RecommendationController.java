package com.cinefy.controller;

import com.cinefy.dto.RecommendationResponse;
import com.cinefy.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public RecommendationResponse recommend(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "6") int limit) {
        return service.recommend(userId, limit);
    }
}
