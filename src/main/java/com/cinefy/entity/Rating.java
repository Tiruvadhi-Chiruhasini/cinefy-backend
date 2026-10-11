package com.cinefy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

/**
 * One user's rating (1-5) of one movie.
 * The unique constraint guarantees a user can rate a given movie at most once.
 */
@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_rating_user_movie", columnNames = {"user_id", "movie_id"}),
        indexes = {
                @Index(name = "idx_ratings_movie", columnList = "movie_id"),
                @Index(name = "idx_ratings_user", columnList = "user_id")
        })
@Check(name = "ck_rating_range", constraints = "rating between 1 and 5")
@Getter
@Setter
@NoArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rating_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rating_movie"))
    private Movie movie;

    @Column(nullable = false)
    private int rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
