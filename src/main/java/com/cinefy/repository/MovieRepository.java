package com.cinefy.repository;

import com.cinefy.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /** Empty strings mean "no filter" (avoids null-parameter typing problems in PostgreSQL). */
    @Query("""
            select m from Movie m
            where lower(m.title) like lower(concat('%', :q, '%'))
              and (:genre = '' or m.genre = :genre)
            """)
    List<Movie> search(@Param("q") String q, @Param("genre") String genre);

    @Query("select distinct m.genre from Movie m order by m.genre")
    List<String> findGenres();

    /** Rows: [genre, movieCount (Long)] */
    @Query("select m.genre, count(m) from Movie m group by m.genre order by count(m) desc, m.genre")
    List<Object[]> countByGenre();

    List<Movie> findTop8ByOrderByCreatedAtDescIdDesc();

    List<Movie> findByGenreAndIdNot(String genre, Long id);
}
