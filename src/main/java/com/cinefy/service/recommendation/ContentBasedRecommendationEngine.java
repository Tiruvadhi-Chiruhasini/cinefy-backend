package com.cinefy.service.recommendation;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Transparent content-based scoring. Every component is normalised to 0..1.
 *
 * <pre>
 * genreAffinity = 0.7 * ((userAvgInGenre - 1) / 4) + 0.3 * (userRatingsInGenre / userRatingsInFavouriteGenre)
 * quality       = ((C * globalMean + avg * n) / (C + n) - 1) / 4        (Bayesian average, C = 5)
 * confidence    = ln(1 + n) / ln(1 + maxN)                              (maxN = most-rated candidate)
 *
 * score (user has history) = 0.50 * genreAffinity + 0.35 * quality + 0.15 * confidence
 * score (new user)         = 0.70 * quality + 0.30 * confidence
 * </pre>
 *
 * The Bayesian average stops a movie with a single 5-star rating from beating a movie with many 4.5-star ratings.
 * This is rule-based scoring, not machine learning or collaborative filtering.
 */
@Component
public class ContentBasedRecommendationEngine implements RecommendationEngine {

    static final double W_AFFINITY = 0.50;
    static final double W_QUALITY = 0.35;
    static final double W_CONFIDENCE = 0.15;
    static final double W_QUALITY_COLD = 0.70;
    static final double W_CONFIDENCE_COLD = 0.30;
    static final double PRIOR_WEIGHT = 5.0;

    @Override
    public List<GenreAffinity> rankGenres(List<GenreStat> userGenres) {
        long maxCount = userGenres.stream().mapToLong(GenreStat::count).max().orElse(0);
        if (maxCount == 0) {
            return List.of();
        }
        return userGenres.stream()
                .filter(g -> g.count() > 0)
                .map(g -> new GenreAffinity(g.genre(), g.count(), g.averageRating(),
                        0.7 * normalize(g.averageRating()) + 0.3 * ((double) g.count() / maxCount)))
                .sorted(Comparator.comparingDouble(GenreAffinity::affinity).reversed()
                        .thenComparing(GenreAffinity::genre))
                .toList();
    }

    @Override
    public List<ScoredMovie> recommend(List<GenreStat> userGenres, List<Candidate> candidates,
                                       double globalMean, int limit) {
        if (candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<GenreAffinity> ranked = rankGenres(userGenres);
        boolean coldStart = ranked.isEmpty();
        Map<String, GenreAffinity> byGenre = new HashMap<>();
        ranked.forEach(g -> byGenre.put(g.genre(), g));
        double mean = globalMean > 0 ? globalMean : 3.0;
        long maxN = candidates.stream().mapToLong(Candidate::ratingCount).max().orElse(0);

        List<Scored> scored = new ArrayList<>();
        for (Candidate c : candidates) {
            GenreAffinity ga = byGenre.get(c.genre());
            double affinity = ga == null ? 0.0 : ga.affinity();
            double quality = normalize(bayesianAverage(c.averageRating(), c.ratingCount(), mean));
            double confidence = maxN == 0 ? 0.0 : Math.log1p(c.ratingCount()) / Math.log1p(maxN);
            double score = coldStart
                    ? W_QUALITY_COLD * quality + W_CONFIDENCE_COLD * confidence
                    : W_AFFINITY * affinity + W_QUALITY * quality + W_CONFIDENCE * confidence;
            scored.add(new Scored(c, round3(clamp01(score)), ga, coldStart));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing((Scored s) -> s.candidate().ratingCount(), Comparator.reverseOrder())
                        .thenComparingLong(s -> s.candidate().movieId()))
                .limit(limit)
                .map(s -> new ScoredMovie(s.candidate().movieId(), s.score(), explain(s)))
                .toList();
    }

    // ---------- helpers (package-private so unit tests can reach them) ----------

    public static double bayesianAverage(double average, long count, double globalMean) {
        return (PRIOR_WEIGHT * globalMean + average * count) / (PRIOR_WEIGHT + count);
    }

    static double normalize(double rating) {
        return clamp01((rating - 1.0) / 4.0);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static String explain(Scored s) {
        Candidate c = s.candidate();
        String community = c.ratingCount() == 0
                ? "No community ratings yet."
                : String.format(Locale.ROOT, "Community rating %.1f/5 from %d %s.",
                c.averageRating(), c.ratingCount(), c.ratingCount() == 1 ? "rating" : "ratings");
        if (s.coldStart()) {
            return "Popular pick to get you started. " + community;
        }
        if (s.genreAffinity() != null) {
            GenreAffinity g = s.genreAffinity();
            return String.format(Locale.ROOT, "Matches your taste in %s (you average %.1f/5 across %d %s). %s",
                    g.genre(), g.averageRating(), g.count(), g.count() == 1 ? "rating" : "ratings", community);
        }
        return String.format(Locale.ROOT, "A chance to explore %s. %s", c.genre(), community);
    }

    private record Scored(Candidate candidate, double score, GenreAffinity genreAffinity, boolean coldStart) {
    }
}
