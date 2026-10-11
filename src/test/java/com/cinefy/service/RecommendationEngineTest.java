package com.cinefy.service;

import com.cinefy.service.recommendation.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEngineTest {

    private final ContentBasedRecommendationEngine engine = new ContentBasedRecommendationEngine();

    @Test
    void favouriteGenreOutranksOtherGenreWithEqualCommunityRating() {
        List<GenreStat> history = List.of(new GenreStat("Drama", 5, 4.8), new GenreStat("Comedy", 2, 2.0));
        List<Candidate> candidates = List.of(
                new Candidate(1, "Comedy", 4.0, 10),
                new Candidate(2, "Drama", 4.0, 10));

        List<ScoredMovie> result = engine.recommend(history, candidates, 3.5, 5);

        assertThat(result).extracting(ScoredMovie::movieId).containsExactly(2L, 1L);
        assertThat(result.get(0).reason()).contains("Drama").contains("4.8");
    }

    @Test
    void manyGoodRatingsBeatOneLuckyFiveStarRating() {
        List<GenreStat> history = List.of(new GenreStat("Drama", 3, 4.0));
        List<Candidate> candidates = List.of(
                new Candidate(1, "Drama", 5.0, 1),
                new Candidate(2, "Drama", 4.6, 50));

        List<ScoredMovie> result = engine.recommend(history, candidates, 3.5, 5);

        assertThat(result.get(0).movieId()).isEqualTo(2L);
    }

    @Test
    void newUserGetsPopularHighQualityMovies() {
        List<Candidate> candidates = List.of(
                new Candidate(1, "Action", 3.0, 4),
                new Candidate(2, "Comedy", 4.5, 30),
                new Candidate(3, "Drama", 4.4, 2));

        List<ScoredMovie> result = engine.recommend(List.of(), candidates, 3.6, 3);

        assertThat(result.get(0).movieId()).isEqualTo(2L);
        assertThat(result.get(0).reason()).startsWith("Popular pick");
    }

    @Test
    void movieWithoutRatingsIsScoredSafely() {
        List<GenreStat> history = List.of(new GenreStat("Sci-Fi", 4, 4.5));
        List<Candidate> candidates = List.of(new Candidate(1, "Sci-Fi", 0.0, 0), new Candidate(2, "Horror", 0.0, 0));

        List<ScoredMovie> result = engine.recommend(history, candidates, 0.0, 5);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r -> {
            assertThat(r.score()).isBetween(0.0, 1.0);
            assertThat(Double.isNaN(r.score())).isFalse();
        });
        assertThat(result.get(0).movieId()).isEqualTo(1L);
        assertThat(result.get(0).reason()).contains("No community ratings yet");
    }

    @Test
    void scoresStayBetweenZeroAndOneAndLimitIsApplied() {
        List<GenreStat> history = List.of(new GenreStat("Action", 10, 5.0));
        List<Candidate> candidates = List.of(
                new Candidate(1, "Action", 5.0, 100),
                new Candidate(2, "Action", 1.0, 100),
                new Candidate(3, "Drama", 1.0, 0),
                new Candidate(4, "Drama", 3.0, 7));

        List<ScoredMovie> result = engine.recommend(history, candidates, 3.0, 2);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r -> assertThat(r.score()).isBetween(0.0, 1.0));
    }

    @Test
    void noCandidatesGivesEmptyResult() {
        assertThat(engine.recommend(List.of(), List.of(), 3.5, 5)).isEmpty();
    }

    @Test
    void genresAreRankedByAffinity() {
        List<GenreAffinity> ranked = engine.rankGenres(List.of(
                new GenreStat("Comedy", 3, 3.0),
                new GenreStat("Drama", 6, 4.7),
                new GenreStat("Horror", 1, 1.0)));

        assertThat(ranked).extracting(GenreAffinity::genre).containsExactly("Drama", "Comedy", "Horror");
        assertThat(ranked.get(0).affinity()).isBetween(0.0, 1.0);
    }

    @Test
    void bayesianAverageMovesTowardsTheMeanWhenThereAreFewRatings() {
        double shrunk = ContentBasedRecommendationEngine.bayesianAverage(5.0, 1, 3.0);
        double established = ContentBasedRecommendationEngine.bayesianAverage(5.0, 100, 3.0);

        assertThat(shrunk).isLessThan(established);
        assertThat(established).isGreaterThan(4.8);
    }
}
