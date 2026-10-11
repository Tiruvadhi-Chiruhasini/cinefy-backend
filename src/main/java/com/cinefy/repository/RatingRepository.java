package com.cinefy.repository;

import com.cinefy.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    /** Rows: [movieId, averageRating (Double), ratingCount (Long)] */
    @Query("select r.movie.id, avg(r.rating), count(r) from Rating r group by r.movie.id")
    List<Object[]> movieStats();

    /** Rows: [userId, ratingCount (Long), averageRating (Double)] */
    @Query("select r.user.id, count(r), avg(r.rating) from Rating r group by r.user.id")
    List<Object[]> userStats();

    /** Rows: [genre, averageRating (Double), ratingCount (Long)] for one user */
    @Query("select m.genre, avg(r.rating), count(r) from Rating r join r.movie m where r.user.id = :userId group by m.genre")
    List<Object[]> genreStatsForUser(@Param("userId") Long userId);

    /** Rows: [genre, averageRating (Double), ratingCount (Long)] across all users */
    @Query("select m.genre, avg(r.rating), count(r) from Rating r join r.movie m group by m.genre")
    List<Object[]> genreStats();

    @Query("select r.movie.id from Rating r where r.user.id = :userId")
    Set<Long> findRatedMovieIds(@Param("userId") Long userId);

    @Query("select coalesce(avg(r.rating), 0.0) from Rating r")
    Double globalAverage();

    /** Rows: [rating (Integer), createdAt (LocalDateTime)], optionally limited to one genre ('' = all). */
    @Query("select r.rating, r.createdAt from Rating r join r.movie m where (:genre = '' or m.genre = :genre)")
    List<Object[]> ratingPoints(@Param("genre") String genre);

    @Query("select r from Rating r join fetch r.user join fetch r.movie where r.user.id = :userId order by r.createdAt desc, r.id desc")
    List<Rating> findByUserWithDetails(@Param("userId") Long userId);

    @Query(value = """
            select r from Rating r join fetch r.user join fetch r.movie
            where (:userId = 0L or r.user.id = :userId)
              and (:movieId = 0L or r.movie.id = :movieId)
            order by r.createdAt desc, r.id desc
            """,
            countQuery = """
            select count(r) from Rating r
            where (:userId = 0L or r.user.id = :userId)
              and (:movieId = 0L or r.movie.id = :movieId)
            """)
    Page<Rating> search(@Param("userId") long userId, @Param("movieId") long movieId, Pageable pageable);

    @Modifying
    @Query("delete from Rating r where r.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
}
