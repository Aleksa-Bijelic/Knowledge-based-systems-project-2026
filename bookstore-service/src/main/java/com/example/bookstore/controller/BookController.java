package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.RatingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookRepository bookRepository;
    private final RatingRepository ratingRepository;

    public BookController(BookRepository bookRepository, RatingRepository ratingRepository) {
        this.bookRepository = bookRepository;
        this.ratingRepository = ratingRepository;
    }

    @GetMapping
    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book saved = bookRepository.save(book);
        return ResponseEntity.created(URI.create("/api/books/" + saved.getId())).body(saved);
    }

    private BookDto mapToDto(Book book) {
        Double average = ratingRepository.findAverageScoreByBookId(book.getId());
        long count = ratingRepository.countByBookId(book.getId());
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getImageUrl(),
                book.getPrice(),
                book.getPublishedDate(),
                book.getAddedAt(),
                average != null ? Math.round(average * 10) / 10.0 : 0.0,
                count
        );
    }

    public static class BookDto {
        private Long id;
        private String title;
        private String author;
        private String genre;
        private String imageUrl;
        private double price;
        private java.time.LocalDate publishedDate;
        private java.time.LocalDate addedAt;
        private double averageRating;
        private long ratingCount;

        public BookDto(Long id, String title, String author, String genre, String imageUrl, double price,
                       java.time.LocalDate publishedDate, java.time.LocalDate addedAt,
                       double averageRating, long ratingCount) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.genre = genre;
            this.imageUrl = imageUrl;
            this.price = price;
            this.publishedDate = publishedDate;
            this.addedAt = addedAt;
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public String getGenre() {
            return genre;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public double getPrice() {
            return price;
        }

        public java.time.LocalDate getPublishedDate() {
            return publishedDate;
        }

        public java.time.LocalDate getAddedAt() {
            return addedAt;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public long getRatingCount() {
            return ratingCount;
        }
    }
}
