package com.example.bookstore.rules;

public class UserPreferredGenre {

    private final String username;
    private final String genre;

    public UserPreferredGenre(String username, String genre) {
        this.username = username;
        this.genre = genre;
    }

    public String getUsername() {
        return username;
    }

    public String getGenre() {
        return genre;
    }
}
