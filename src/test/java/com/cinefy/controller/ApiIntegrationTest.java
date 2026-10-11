package com.cinefy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real application against in-memory H2 with the sample data seeded. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void seedDataIsAvailableThroughMovieList() throws Exception {
        mvc.perform(get("/api/movies").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(40)));
    }

    @Test
    void movieSearchAndGenreFilterWork() throws Exception {
        mvc.perform(get("/api/movies").param("genre", "Horror"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].genre", everyItem(is("Horror"))));
        mvc.perform(get("/api/movies").param("q", "harbor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", containsStringIgnoringCase("harbor")));
    }

    @Test
    void unknownMovieReturns404() throws Exception {
        mvc.perform(get("/api/movies/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void invalidMovieIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/movies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"genre\":\"Drama\",\"releaseYear\":2020}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void ratingLifecycleCreateDuplicateUpdateDelete() throws Exception {
        MvcResult created = mvc.perform(post("/api/movies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Film\",\"genre\":\"Drama\",\"releaseYear\":2024}"))
                .andExpect(status().isCreated())
                .andReturn();
        long movieId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String body = "{\"userId\":1,\"movieId\":" + movieId + ",\"rating\":4}";

        MvcResult rating = mvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andReturn();
        tools.jackson.databind.JsonNode node = json.readTree(rating.getResponse().getContentAsString());
        long ratingId = node.get("id").asLong();

        mvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"movieId\":" + movieId + ",\"rating\":6}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/ratings/" + ratingId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2));

        mvc.perform(get("/api/movies/" + movieId))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.averageRating").value(2.0));

        mvc.perform(delete("/api/ratings/" + ratingId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/movies/" + movieId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/movies/" + movieId)).andExpect(status().isNotFound());
    }

    @Test
    void analyticsOverviewIsCalculatedFromDatabase() throws Exception {
        mvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovies", greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.totalRatings", greaterThanOrEqualTo(150)))
                .andExpect(jsonPath("$.recentMovies", not(empty())));
        mvc.perform(get("/api/analytics/genres")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));
        mvc.perform(get("/api/analytics/ratings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution", hasSize(5)))
                .andExpect(jsonPath("$.trend", not(empty())));
        mvc.perform(get("/api/analytics/top-movies").param("limit", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));
        mvc.perform(get("/api/analytics/popular-movies").param("limit", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void recommendationsExcludeMoviesAlreadyRatedAndExplainWhy() throws Exception {
        MvcResult result = mvc.perform(get("/api/recommendations/2").param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations", hasSize(6)))
                .andExpect(jsonPath("$.recommendations[0].reason", not(emptyString())))
                .andReturn();

        tools.jackson.databind.JsonNode root = json.readTree(result.getResponse().getContentAsString());
        MvcResult rated = mvc.perform(get("/api/ratings").param("userId", "2").param("size", "100")).andReturn();
        String ratedJson = rated.getResponse().getContentAsString();
        for (tools.jackson.databind.JsonNode rec : root.get("recommendations")) {
            long recId = rec.get("movie").get("id").asLong();
            org.assertj.core.api.Assertions.assertThat(ratedJson).doesNotContain("\"movieId\":" + recId + ",");
        }
    }

    @Test
    void usersEndpointHidesSensitiveFields() throws Exception {
        mvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.preferredGenres", not(empty())));
    }
}
