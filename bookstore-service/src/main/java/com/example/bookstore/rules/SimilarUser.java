package com.example.bookstore.rules;

public class SimilarUser {

    private final String username;
    private final String similarUsername;
    private final double correlation;

    public SimilarUser(String username, String similarUsername, double correlation) {
        this.username = username;
        this.similarUsername = similarUsername;
        this.correlation = correlation;
    }

    public String getUsername() {
        return username;
    }

    public String getSimilarUsername() {
        return similarUsername;
    }

    public double getCorrelation() {
        return correlation;
    }
}
