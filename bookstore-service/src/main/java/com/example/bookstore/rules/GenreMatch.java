package com.example.bookstore.rules;

public class GenreMatch {

    private final String author;
    private final String genre;

    public GenreMatch(String author, String genre) {
        this.author = author;
        this.genre = genre;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }
}
