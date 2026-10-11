package com.cinefy.controller;

import com.cinefy.dto.PageResponse;
import com.cinefy.dto.RatingRequest;
import com.cinefy.dto.RatingResponse;
import com.cinefy.dto.RatingUpdateRequest;
import com.cinefy.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<RatingResponse> list(@RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) Long movieId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.list(userId, movieId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse create(@Valid @RequestBody RatingRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public RatingResponse update(@PathVariable Long id, @Valid @RequestBody RatingUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
