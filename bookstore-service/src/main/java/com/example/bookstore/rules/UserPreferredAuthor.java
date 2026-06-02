package com.example.bookstore.rules;

public class UserPreferredAuthor {

    private final String username;
    private final String author;

    public UserPreferredAuthor(String username, String author) {
        this.username = username;
        this.author = author;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthor() {
        return author;
    }
}
