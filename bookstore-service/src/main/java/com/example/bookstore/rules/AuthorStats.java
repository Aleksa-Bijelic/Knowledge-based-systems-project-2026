package com.example.bookstore.rules;

public class AuthorStats {

    private final String author;
    private final long totalBooks;
    private long totalRatingCount;

    public AuthorStats(String author, long totalBooks, long totalRatingCount) {
        this.author = author;
        this.totalBooks = totalBooks;
        this.totalRatingCount = totalRatingCount;
    }

    public String getAuthor() {
        return author;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public long getTotalRatingCount() {
        return totalRatingCount;
    }

    public void setTotalRatingCount(long totalRatingCount) {
        this.totalRatingCount = totalRatingCount;
    }
}
