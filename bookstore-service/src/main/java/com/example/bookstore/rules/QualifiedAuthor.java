package com.example.bookstore.rules;

public class QualifiedAuthor {

    private final String author;
    private final long totalRatingCount;
    private boolean selected;

    public QualifiedAuthor(String author, long totalRatingCount) {
        this.author = author;
        this.totalRatingCount = totalRatingCount;
        this.selected = false;
    }

    public String getAuthor() {
        return author;
    }

    public long getTotalRatingCount() {
        return totalRatingCount;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
