package com.example.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int score;

    @Column(length = 1024)
    private String comment;

    @Column(name = "rated_at", nullable = false)
    private LocalDateTime ratedAt;

    public Rating() {
    }

    public Rating(Book book, String username, int score, String comment, LocalDateTime ratedAt) {
        this.book = book;
        this.username = username;
        this.score = score;
        this.comment = comment;
        this.ratedAt = ratedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getRatedAt() {
        return ratedAt;
    }

    public void setRatedAt(LocalDateTime ratedAt) {
        this.ratedAt = ratedAt;
    }
}
