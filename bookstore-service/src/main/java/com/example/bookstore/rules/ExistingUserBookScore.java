package com.example.bookstore.rules;

import java.time.LocalDate;

public class ExistingUserBookScore {

    private final Long bookId;
    private final String bookTitle;
    private final LocalDate publishedDate;
    private int score;
    private boolean selected;
    private boolean criterion1Met;
    private boolean criterion2Met;
    private boolean criterion3Met;

    public ExistingUserBookScore(Long bookId, String bookTitle, LocalDate publishedDate) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.publishedDate = publishedDate;
        this.score = 0;
        this.selected = false;
        this.criterion1Met = false;
        this.criterion2Met = false;
        this.criterion3Met = false;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isCriterion1Met() {
        return criterion1Met;
    }

    public void setCriterion1Met(boolean criterion1Met) {
        this.criterion1Met = criterion1Met;
    }

    public boolean isCriterion2Met() {
        return criterion2Met;
    }

    public void setCriterion2Met(boolean criterion2Met) {
        this.criterion2Met = criterion2Met;
    }

    public boolean isCriterion3Met() {
        return criterion3Met;
    }

    public void setCriterion3Met(boolean criterion3Met) {
        this.criterion3Met = criterion3Met;
    }
}
