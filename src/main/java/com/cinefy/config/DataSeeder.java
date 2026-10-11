package com.cinefy.config;

import com.cinefy.entity.Movie;
import com.cinefy.entity.Rating;
import com.cinefy.entity.User;
import com.cinefy.repository.MovieRepository;
import com.cinefy.repository.RatingRepository;
import com.cinefy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Inserts SAMPLE data (fictional movie titles, made-up users) the first time the app starts on an empty database.
 * The random generator uses a fixed seed, so every fresh database gets the same ratings.
 * Disable with SEED_ENABLED=false.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] GENRES = {"Action", "Comedy", "Drama", "Sci-Fi", "Thriller",
            "Romance", "Animation", "Horror", "Fantasy", "Documentary"};

    /** title | genre index | year | description (all fictional) */
    private static final String[] MOVIES = {
            "Iron Meridian|0|2019|A retired pilot is pulled back in when a cargo flight vanishes over the Arctic.",
            "Last Convoy|0|2021|Six drivers must cross a collapsing border road before the bridges close.",
            "Crimson Rooftop|0|2016|A parkour courier discovers the package she carries is a ransom note.",
            "Border Run|0|2023|Two rival smugglers share one truck and one night to reach the coast.",
            "The Wrong Wedding Guest|1|2018|A man crashes the wrong ceremony and accidentally becomes the best man.",
            "Office Hours Overdue|1|2022|A night-shift cleaner is mistaken for the new company CEO.",
            "Grandpa's Road Trip|1|2015|Three grandchildren drive a stubborn grandfather to his last fishing spot.",
            "Two Left Feet|1|2020|A shy accountant enters a dance contest to win back his lost bet.",
            "A Quiet Harbor|2|2017|A fishing family decides whether to sell the boat that built their town.",
            "The Long Autumn|2|2021|Two estranged sisters sort their late mother's house over one season.",
            "Paper Houses|2|2014|A teacher in a flood-prone village rebuilds her school with the children.",
            "Where the River Bends|2|2023|A ferry captain keeps a promise made to a stranger decades ago.",
            "Orbit of Strangers|3|2020|Five strangers wake on a drifting station with no record of boarding.",
            "Signal From Kepler Ridge|3|2022|A radio astronomer decodes a message that seems to answer her own voice.",
            "The Memory Archive|3|2018|In a city that stores memories, an archivist finds one that was deleted.",
            "Glass Horizon|3|2024|A climate engineer races to restart a sea-wall grid before the tide.",
            "Seventh Floor|4|2019|An insurance auditor notices an apartment that is not on any floor plan.",
            "No Witnesses Left|4|2016|A court stenographer realises she has transcribed a confession twice.",
            "The Night Ledger|4|2022|A bank clerk finds an account that records tomorrow's transactions.",
            "Cold Alibi|4|2013|A detective's perfect alibi is the only thing that does not add up.",
            "Letters to Lisbon|5|2018|Two pen pals plan to meet on the same bridge, a year apart.",
            "Monsoon Evenings|5|2021|A bookshop owner and a rain-stranded cyclist trade stories until dawn.",
            "The Second First Date|5|2023|A couple agrees to meet again as strangers to see what happens.",
            "Winter in Verona|5|2015|A translator falls for the actor she is hired to coach.",
            "Pip and the Paper Moon|6|2019|A paper doll folds her way across the night sky to find the moon.",
            "Robo Bakery|6|2022|A tiny robot learns that the best bread needs patience, not speed.",
            "The Tiny Cartographer|6|2017|A mouse maps the house to find her way back to her family.",
            "Cloud Harbor|6|2024|A cloud-shaped whale helps a lonely lighthouse keeper's grandson.",
            "The Hollow Stair|7|2018|A new tenant counts thirteen steps on a staircase built for twelve.",
            "Static Lullaby|7|2021|A baby monitor starts playing a song nobody in the house knows.",
            "Room Nine|7|2016|A motel guest finds every room key opens the same door.",
            "What the Fog Kept|7|2023|A coastal village forgets one resident each time the fog arrives.",
            "The Ember Crown|8|2017|A blacksmith's daughter inherits a crown that only glows for liars.",
            "Moonlit Apprentice|8|2020|A clockmaker's apprentice discovers the town clock controls the tides.",
            "Kingdom of Salt and Thread|8|2022|Two weavers stitch a map that rewrites a drowned kingdom's borders.",
            "The Last Lantern Keeper|8|2014|The final lantern keeper guards the only road out of the dark forest.",
            "Concrete Gardens|9|2020|Rooftop farmers reshape a dense city one vegetable bed at a time.",
            "Tides of Tamarind|9|2018|A coastal community documents its changing shoreline across ten years.",
            "The Chess Cafe|9|2016|Regulars of a neighbourhood cafe talk about a game that outlasts careers.",
            "Mountains Remember|9|2023|Elders retrace old trade routes through a high mountain pass."
    };

    private static final String[] USERS = {
            "Aarav Mehta", "Diya Rao", "Kabir Shah", "Ananya Iyer", "Rohan Das", "Meera Nair",
            "Vikram Singh", "Isha Kapoor", "Arjun Reddy", "Sara Khan", "Nikhil Verma", "Priya Menon",
            "Dev Patel", "Tara Bose", "Karan Malhotra", "Neha Joshi", "Imran Ali", "Sneha Pillai"
    };

    private final MovieRepository movies;
    private final UserRepository users;
    private final RatingRepository ratings;
    private final boolean enabled;

    public DataSeeder(MovieRepository movies, UserRepository users, RatingRepository ratings,
                      @Value("${cinefy.seed.enabled:true}") boolean enabled) {
        this.movies = movies;
        this.users = users;
        this.ratings = ratings;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("Sample data seeding is disabled.");
            return;
        }
        if (movies.count() > 0 || users.count() > 0 || ratings.count() > 0) {
            log.info("Database already contains data; skipping sample data.");
            return;
        }

        Random rnd = new Random(42);
        LocalDateTime now = LocalDateTime.now();

        List<Movie> savedMovies = new ArrayList<>();
        for (int i = 0; i < MOVIES.length; i++) {
            String[] p = MOVIES[i].split("\\|");
            Movie m = new Movie();
            m.setTitle(p[0]);
            m.setGenre(GENRES[Integer.parseInt(p[1])]);
            m.setReleaseYear(Integer.parseInt(p[2]));
            m.setDescription(p[3]);
            // Spread creation dates so "recently added" is meaningful.
            m.setCreatedAt(now.minusDays((MOVIES.length - i) * 2L).minusHours(rnd.nextInt(12)));
            savedMovies.add(m);
        }
        movies.saveAll(savedMovies);

        List<User> savedUsers = new ArrayList<>();
        for (String name : USERS) {
            User u = new User();
            u.setName(name);
            u.setEmail(name.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com");
            u.setCreatedAt(now.minusDays(30 + rnd.nextInt(300)));
            savedUsers.add(u);
        }
        users.saveAll(savedUsers);

        List<Rating> batch = new ArrayList<>();
        for (int i = 0; i < savedUsers.size(); i++) {
            User user = savedUsers.get(i);
            String fav1 = GENRES[i % GENRES.length];
            String fav2 = GENRES[(i * 3 + 4) % GENRES.length];
            if (fav2.equals(fav1)) {
                fav2 = GENRES[(i + 1) % GENRES.length];
            }
            String disliked = GENRES[(i + 5) % GENRES.length];

            List<Movie> favourites = new ArrayList<>();
            List<Movie> others = new ArrayList<>();
            for (Movie m : savedMovies) {
                (m.getGenre().equals(fav1) || m.getGenre().equals(fav2) ? favourites : others).add(m);
            }
            Collections.shuffle(favourites, rnd);
            Collections.shuffle(others, rnd);

            List<Movie> picks = new ArrayList<>(favourites.subList(0, Math.min(6, favourites.size())));
            picks.addAll(others.subList(0, 12 - picks.size()));

            for (Movie m : picks) {
                int value;
                if (m.getGenre().equals(fav1) || m.getGenre().equals(fav2)) {
                    value = 4 + (rnd.nextInt(10) < 5 ? 1 : 0);
                } else if (m.getGenre().equals(disliked)) {
                    value = 1 + rnd.nextInt(3);
                } else {
                    value = 2 + rnd.nextInt(3);
                }
                Rating r = new Rating();
                r.setUser(user);
                r.setMovie(m);
                r.setRating(value);
                r.setCreatedAt(now.minusDays(rnd.nextInt(330)).minusHours(rnd.nextInt(24)));
                batch.add(r);
            }
        }
        ratings.saveAll(batch);
        log.info("Seeded SAMPLE data: {} movies, {} users, {} ratings.",
                savedMovies.size(), savedUsers.size(), batch.size());
    }
}
