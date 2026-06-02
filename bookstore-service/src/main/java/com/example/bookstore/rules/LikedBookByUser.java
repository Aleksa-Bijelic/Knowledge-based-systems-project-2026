package com.example.bookstore.rules;

public class LikedBookByUser {

    private final String username;
    private final Long bookId;

    public LikedBookByUser(String username, Long bookId) {
        this.username = username;
        this.bookId = bookId;
    }

    public String getUsername() {
        return username;
    }

    public Long getBookId() {
        return bookId;
    }
}
