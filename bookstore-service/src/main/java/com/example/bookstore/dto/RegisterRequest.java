package com.example.bookstore.dto;

import java.util.HashSet;
import java.util.Set;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private Set<String> favoriteGenres = new HashSet<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(Set<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres != null ? favoriteGenres : new HashSet<>();
    }
}
