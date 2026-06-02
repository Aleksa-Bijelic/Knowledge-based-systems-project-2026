package com.example.bookstore.rules;

public class BookPairSimilarity {

    private final Long bookId1;
    private final Long bookId2;

    public BookPairSimilarity(Long bookId1, Long bookId2) {
        this.bookId1 = bookId1;
        this.bookId2 = bookId2;
    }

    public Long getBookId1() {
        return bookId1;
    }

    public Long getBookId2() {
        return bookId2;
    }
}
