package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.Rating;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.RatingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/books")
public class RatingController {

    private final BookRepository bookRepository;
    private final RatingRepository ratingRepository;

    public RatingController(BookRepository bookRepository, RatingRepository ratingRepository) {
        this.bookRepository = bookRepository;
        this.ratingRepository = ratingRepository;
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailDto> getBookDetails(@PathVariable("bookId") Long bookId) {
        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Book book = bookOptional.get();
        List<Rating> ratings = ratingRepository.findByBookId(bookId);
        Double average = ratingRepository.findAverageScoreByBookId(bookId);
        long count = ratingRepository.countByBookId(bookId);

        BookDetailDto dto = new BookDetailDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getImageUrl(),
                book.getPrice(),
                book.getPublishedDate(),
                book.getAddedAt(),
                average != null ? Math.round(average * 10) / 10.0 : 0.0,
                count,
                ratings.stream().map(this::mapRating).collect(Collectors.toList())
        );

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{bookId}/ratings")
    public ResponseEntity<Rating> addRating(
            @PathVariable("bookId") Long bookId,
            @RequestBody Rating ratingRequest
    ) {
        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if ("admin".equalsIgnoreCase(ratingRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin users cannot submit reviews.");
        }

        if (ratingRepository.existsByBookIdAndUsername(bookId, ratingRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this book.");
        }

        Book book = bookOptional.get();
        Rating rating = new Rating();
        rating.setBook(book);
        rating.setUsername(ratingRequest.getUsername());
        rating.setScore(ratingRequest.getScore());
        rating.setComment(ratingRequest.getComment());
        rating.setRatedAt(LocalDateTime.now());

        Rating saved = ratingRepository.save(rating);
        return ResponseEntity.created(URI.create("/api/books/" + bookId + "/ratings/" + saved.getId())).body(saved);
    }

    private RatingDto mapRating(Rating rating) {
        return new RatingDto(
                rating.getId(),
                rating.getUsername(),
                rating.getScore(),
                rating.getComment(),
                rating.getRatedAt()
        );
    }

    public static class BookDetailDto {
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
        private List<RatingDto> ratings;

        public BookDetailDto(Long id, String title, String author, String genre, String imageUrl, double price,
                             java.time.LocalDate publishedDate, java.time.LocalDate addedAt,
                             double averageRating, long ratingCount, List<RatingDto> ratings) {
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
            this.ratings = ratings;
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

        public List<RatingDto> getRatings() {
            return ratings;
        }
    }

    public static class RatingDto {
        private Long id;
        private String username;
        private int score;
        private String comment;
        private java.time.LocalDateTime ratedAt;

        public RatingDto(Long id, String username, int score, String comment, java.time.LocalDateTime ratedAt) {
            this.id = id;
            this.username = username;
            this.score = score;
            this.comment = comment;
            this.ratedAt = ratedAt;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public int getScore() {
            return score;
        }

        public String getComment() {
            return comment;
        }

        public java.time.LocalDateTime getRatedAt() {
            return ratedAt;
        }
    }
}
