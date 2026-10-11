package com.cinefy.service;

/** Community rating statistics for one movie. */
public record MovieStat(double average, long count) {
    public static final MovieStat NONE = new MovieStat(0.0, 0);
}
