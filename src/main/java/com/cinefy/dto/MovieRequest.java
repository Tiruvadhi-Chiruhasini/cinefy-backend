package com.cinefy.dto;

import jakarta.validation.constraints.*;

public record MovieRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title must be at most 200 characters.")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters.")
        String description,

        @NotBlank(message = "Genre is required.")
        @Size(max = 50, message = "Genre must be at most 50 characters.")
        String genre,

        @NotNull(message = "Release year is required.")
        @Min(value = 1888, message = "Release year must be 1888 or later.")
        @Max(value = 2100, message = "Release year must be 2100 or earlier.")
        Integer releaseYear,

        @Size(max = 500, message = "Poster URL must be at most 500 characters.")
        String posterUrl
) {
}
