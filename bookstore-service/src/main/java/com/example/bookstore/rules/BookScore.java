package com.example.bookstore.rules;

public class BookScore {

    private final Long bookId;
    private final double avgScore;
    private boolean selected;

    public BookScore(Long bookId, double avgScore) {
        this.bookId = bookId;
        this.avgScore = avgScore;
        this.selected = false;
    }

    public Long getBookId() {
        return bookId;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
