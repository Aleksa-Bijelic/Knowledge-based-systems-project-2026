package com.example.bookstore.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserRecommendationFact {

    private final String username;
    private boolean newUser;
    private boolean genreSelectionMade;
    private boolean useUnauthenticatedRecommendation;
    private Set<String> favoriteGenres;
    private List<Long> recommendedBookIds;

    public UserRecommendationFact(String username) {
        this.username = username;
        this.newUser = false;
        this.genreSelectionMade = false;
        this.useUnauthenticatedRecommendation = false;
        this.recommendedBookIds = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public void setNewUser(boolean newUser) {
        this.newUser = newUser;
    }

    public boolean isGenreSelectionMade() {
        return genreSelectionMade;
    }

    public void setGenreSelectionMade(boolean genreSelectionMade) {
        this.genreSelectionMade = genreSelectionMade;
    }

    public boolean isUseUnauthenticatedRecommendation() {
        return useUnauthenticatedRecommendation;
    }

    public void setUseUnauthenticatedRecommendation(boolean useUnauthenticatedRecommendation) {
        this.useUnauthenticatedRecommendation = useUnauthenticatedRecommendation;
    }

    public Set<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(Set<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public List<Long> getRecommendedBookIds() {
        return recommendedBookIds;
    }

    public void setRecommendedBookIds(List<Long> recommendedBookIds) {
        this.recommendedBookIds = recommendedBookIds;
    }
}