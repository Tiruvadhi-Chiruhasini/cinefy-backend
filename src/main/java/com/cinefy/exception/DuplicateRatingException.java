package com.cinefy.exception;

public class DuplicateRatingException extends RuntimeException {
    public DuplicateRatingException() {
        super("This user has already rated this movie. Update the existing rating instead.");
    }
}
