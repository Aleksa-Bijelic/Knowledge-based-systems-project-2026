package com.example.bookstore.rules;

import com.example.bookstore.model.Book;
import java.time.LocalDate;

public class BookRecommendationFact {

    private final Book book;
    private final LocalDate addedAt;
    private final LocalDate publishedDate;
    private final LocalDate evaluationDate;
    private boolean newBook;
    private boolean popular;
    private String ratingCategory;
    private boolean recommended;
    private Double randomScore;
    private boolean excluded;

    public BookRecommendationFact(Book book, LocalDate addedAt, LocalDate publishedDate, LocalDate evaluationDate) {
        this.book = book;
        this.addedAt = addedAt;
        this.publishedDate = publishedDate;
        this.evaluationDate = evaluationDate;
        this.excluded = false;
    }

    public Book getBook() {
        return book;
    }

    public LocalDate getAddedAt() {
        return addedAt;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public LocalDate getEvaluationDate() {
        return evaluationDate;
    }

    public boolean isNewBook() {
        return newBook;
    }

    public void setNewBook(boolean newBook) {
        this.newBook = newBook;
    }

    public boolean isPopular() {
        return popular;
    }

    public void setPopular(boolean popular) {
        this.popular = popular;
    }

    public String getRatingCategory() {
        return ratingCategory;
    }

    public void setRatingCategory(String ratingCategory) {
        this.ratingCategory = ratingCategory;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public Double getRandomScore() {
        return randomScore;
    }

    public void setRandomScore(Double randomScore) {
        this.randomScore = randomScore;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }
}
