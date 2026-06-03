package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.RatingRepository;
import com.example.bookstore.rules.BookRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookRepository bookRepository;
    private final RatingRepository ratingRepository;
    private final BookRecommendationService bookRecommendationService;

    public BookController(BookRepository bookRepository, RatingRepository ratingRepository, BookRecommendationService bookRecommendationService) {
        this.bookRepository = bookRepository;
        this.ratingRepository = ratingRepository;
        this.bookRecommendationService = bookRecommendationService;
    }

    @GetMapping
    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/recommendations")
    public List<BookDto> getRecommendedBooks() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                String username = userDetails.getUsername();
                return bookRecommendationService.getRecommendationsForUser(username).stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList());
            }
            return bookRecommendationService.getRecommendationsForAnonymousUser().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting recommendations", e);
            return Collections.emptyList();
        }
    }

    private static final String DEFAULT_IMAGE_URL = "https://www.klett-cotta.de/assets/default-image.jpg";

    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        if (book.getImageUrl() == null || book.getImageUrl().trim().isEmpty()) {
            book.setImageUrl(DEFAULT_IMAGE_URL);
        }
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
