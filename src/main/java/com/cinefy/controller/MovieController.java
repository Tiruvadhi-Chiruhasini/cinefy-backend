package com.cinefy.controller;

import com.cinefy.dto.MovieRequest;
import com.cinefy.dto.MovieResponse;
import com.cinefy.dto.PageResponse;
import com.cinefy.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService service;

    public MovieController(MovieService service) {
        this.service = service;
    }

    /** sort: title | year | rating | popularity;  dir: asc | desc */
    @GetMapping
    public PageResponse<MovieResponse> list(@RequestParam(defaultValue = "") String q,
                                            @RequestParam(defaultValue = "") String genre,
                                            @RequestParam(defaultValue = "title") String sort,
                                            @RequestParam(required = false) String dir,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        return service.list(q, genre, sort, dir, page, size);
    }

    @GetMapping("/genres")
    public List<String> genres() {
        return service.genres();
    }

    @GetMapping("/{id}")
    public MovieResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/related")
    public List<MovieResponse> related(@PathVariable Long id, @RequestParam(defaultValue = "4") int limit) {
        return service.related(id, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse create(@Valid @RequestBody MovieRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MovieResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
