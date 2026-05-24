package com.example.bookstore.repository;

import com.example.bookstore.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.book.id = :bookId")
    Double findAverageScoreByBookId(@Param("bookId") Long bookId);

    long countByBookId(Long bookId);

    boolean existsByBookIdAndUsername(Long bookId, String username);

    List<Rating> findByBookId(Long bookId);
}
