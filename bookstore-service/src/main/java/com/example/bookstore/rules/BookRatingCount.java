package com.example.bookstore.rules;

public class BookRatingCount {

    private final Long bookId;
    private final long count;

    public BookRatingCount(Long bookId, long count) {
        this.bookId = bookId;
        this.count = count;
    }

    public Long getBookId() {
        return bookId;
    }

    public long getCount() {
        return count;
    }
}
