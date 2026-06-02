package com.example.bookstore.rules;

public class UserRecommendationFact {

    private final String username;
    private boolean newUser;
    private boolean genreSelectionMade;
    private boolean useUnauthenticatedRecommendation;

    public UserRecommendationFact(String username) {
        this.username = username;
        this.newUser = false;
        this.genreSelectionMade = false;
        this.useUnauthenticatedRecommendation = false;
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
}