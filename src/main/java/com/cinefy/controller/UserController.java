package com.cinefy.controller;

import com.cinefy.dto.PageResponse;
import com.cinefy.dto.UserDetailResponse;
import com.cinefy.dto.UserResponse;
import com.cinefy.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "") String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public UserDetailResponse get(@PathVariable Long id) {
        return service.get(id);
    }
}
