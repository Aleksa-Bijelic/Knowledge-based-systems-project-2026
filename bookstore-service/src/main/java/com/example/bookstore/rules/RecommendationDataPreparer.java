package com.example.bookstore.rules;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;
import com.example.bookstore.model.Rating;
import org.kie.api.runtime.KieRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendationDataPreparer {

    private static final Logger log = LoggerFactory.getLogger(RecommendationDataPreparer.class);
    private static final double PEARSON_THRESHOLD = 0.5;
    private static final int PREFERRED_AUTHOR_MIN_QUANTITY = 3;
    private static final double PREFERRED_GENRE_RATIO = 0.3;
    private static final int MIN_COMMON_BOOKS_FOR_PEARSON = 2;

    public static void prepareForExistingUser(KieRuntime kieSession,
                                              String username,
                                              List<Rating> allRatings,
                                              List<Order> allOrders,
                                              List<Book> allBooks) {
        log.info("[HELPER] prepareForExistingUser start: user={} ratings={} orders={} books={}",
                username, allRatings.size(), allOrders.size(), allBooks.size());

        Set<String> otherUsernames = new HashSet<>();
        for (Rating r : allRatings) {
            if (r != null && r.getUsername() != null && !r.getUsername().equals(username)) {
                otherUsernames.add(r.getUsername());
            }
        }
        log.info("[HELPER] Other usernames to compare with: {}", otherUsernames.size());

        int similarUserCount = 0;
        for (String otherUser : otherUsernames) {
            double p = pearson(allRatings, username, otherUser);
            if (p >= PEARSON_THRESHOLD) {
                kieSession.insert(new SimilarUser(username, otherUser, p));
                similarUserCount++;
                log.info("[HELPER] Similar user: {} <-> {} pearson={}", username, otherUser, p);
            }
        }
        log.info("[HELPER] Similar users found: {}", similarUserCount);

        Set<Long> bookIds = new HashSet<>();
        for (Rating r : allRatings) {
            if (r != null && r.getBook() != null && r.getBook().getId() != null) {
                bookIds.add(r.getBook().getId());
            }
        }
        List<Long> bookIdList = new ArrayList<>(bookIds);
        int pairCount = 0;
        for (int i = 0; i < bookIdList.size(); i++) {
            for (int j = i + 1; j < bookIdList.size(); j++) {
                Long id1 = bookIdList.get(i);
                Long id2 = bookIdList.get(j);
                if (id1 != null && id2 != null && areBooksSimilar(allRatings, id1, id2)) {
                    kieSession.insert(new BookPairSimilarity(id1, id2));
                    pairCount++;
                }
            }
        }
        log.info("[HELPER] Book pairs (similar): {} from {} candidates", pairCount, bookIdList.size());

        Set<String> prefAuthors = getPreferredAuthors(allOrders, username);
        for (String author : prefAuthors) {
            kieSession.insert(new UserPreferredAuthor(username, author));
        }
        log.info("[HELPER] Preferred authors: {}", prefAuthors);

        Set<String> prefGenres = getPreferredGenres(allOrders, username);
        for (String genre : prefGenres) {
            kieSession.insert(new UserPreferredGenre(username, genre));
        }
        log.info("[HELPER] Preferred genres: {}", prefGenres);

        for (Order order : allOrders) {
            if (order != null
                && username.equals(order.getCustomerUsername())
                && order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item != null && item.getBook() != null && item.getBook().getId() != null) {
                        kieSession.insert(new LikedBookByUser(username, item.getBook().getId()));
                    }
                }
            }
        }

        int highRatingCount = 0;
        for (Rating r : allRatings) {
            if (r != null && r.getScore() >= 4 && r.getBook() != null && r.getBook().getId() != null) {
                kieSession.insert(new LikedBookByUser(r.getUsername(), r.getBook().getId()));
                highRatingCount++;
            }
        }
        log.info("[HELPER] High rating liked books inserted: {}", highRatingCount);

        int bookScoreCount = 0;
        for (Book book : allBooks) {
            if (book != null) {
                kieSession.insert(new ExistingUserBookScore(book.getId(), book.getTitle(), book.getPublishedDate()));
                bookScoreCount++;
            }
        }
        log.info("[HELPER] ExistingUserBookScore inserted: {}", bookScoreCount);
    }

    private static double pearson(List<Rating> allRatings, String userA, String userB) {
        if (allRatings == null || userA == null || userB == null) return 0;
        Map<Long, Integer> ratingsA = new HashMap<>();
        Map<Long, Integer> ratingsB = new HashMap<>();

        for (Rating r : allRatings) {
            if (r == null || r.getBook() == null || r.getBook().getId() == null || r.getUsername() == null) continue;
            Long bookId = r.getBook().getId();
            if (userA.equals(r.getUsername())) {
                ratingsA.put(bookId, r.getScore());
            } else if (userB.equals(r.getUsername())) {
                ratingsB.put(bookId, r.getScore());
            }
        }

        List<Long> commonBooks = new ArrayList<>();
        for (Long key : ratingsA.keySet()) {
            if (ratingsB.containsKey(key)) {
                commonBooks.add(key);
            }
        }

        if (commonBooks.size() < MIN_COMMON_BOOKS_FOR_PEARSON) return 0;

        double meanA = 0, meanB = 0;
        for (Long bookId : commonBooks) {
            meanA += ratingsA.get(bookId);
            meanB += ratingsB.get(bookId);
        }
        meanA /= commonBooks.size();
        meanB /= commonBooks.size();

        double num = 0, sumSqA = 0, sumSqB = 0;
        for (Long bookId : commonBooks) {
            double diffA = ratingsA.get(bookId) - meanA;
            double diffB = ratingsB.get(bookId) - meanB;
            num += diffA * diffB;
            sumSqA += diffA * diffA;
            sumSqB += diffB * diffB;
        }

        double denom = Math.sqrt(sumSqA) * Math.sqrt(sumSqB);
        if (denom == 0) return 0;
        return num / denom;
    }

    private static boolean areBooksSimilar(List<Rating> allRatings, Long bookId1, Long bookId2) {
        if (allRatings == null || bookId1 == null || bookId2 == null) return false;
        Map<String, Integer> scores1 = new HashMap<>();
        Map<String, Integer> scores2 = new HashMap<>();

        for (Rating r : allRatings) {
            if (r == null || r.getBook() == null || r.getBook().getId() == null || r.getUsername() == null) continue;
            Long bid = r.getBook().getId();
            if (bid.equals(bookId1)) {
                scores1.put(r.getUsername(), r.getScore());
            } else if (bid.equals(bookId2)) {
                scores2.put(r.getUsername(), r.getScore());
            }
        }

        int similar = 0, total = 0;
        for (Map.Entry<String, Integer> entry : scores1.entrySet()) {
            Integer s2 = scores2.get(entry.getKey());
            if (s2 != null) {
                total++;
                if (Math.abs(entry.getValue() - s2) <= 1) similar++;
            }
        }

        return total > 0 && (double) similar / total >= 0.7;
    }

    private static Set<String> getPreferredAuthors(List<Order> allOrders, String username) {
        Set<String> result = new HashSet<>();
        if (allOrders == null || username == null) return result;
        Map<String, Integer> authorCount = new HashMap<>();
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        for (Order order : allOrders) {
            if (order != null
                && username.equals(order.getCustomerUsername())
                && order.getCreatedAt() != null
                && !order.getCreatedAt().isBefore(sixMonthsAgo)
                && order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item != null && item.getBook() != null) {
                        String author = item.getBook().getAuthor();
                        if (author != null) {
                            authorCount.merge(author, item.getQuantity(), Integer::sum);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : authorCount.entrySet()) {
            if (entry.getValue() >= PREFERRED_AUTHOR_MIN_QUANTITY) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Set<String> getPreferredGenres(List<Order> allOrders, String username) {
        Set<String> result = new HashSet<>();
        if (allOrders == null || username == null) return result;
        Map<String, Integer> genreCount = new HashMap<>();
        int total = 0;
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        for (Order order : allOrders) {
            if (order != null
                && username.equals(order.getCustomerUsername())
                && order.getCreatedAt() != null
                && !order.getCreatedAt().isBefore(sixMonthsAgo)
                && order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item != null && item.getBook() != null) {
                        String genre = item.getBook().getGenre();
                        if (genre != null) {
                            genreCount.merge(genre, item.getQuantity(), Integer::sum);
                            total += item.getQuantity();
                        }
                    }
                }
            }
        }

        if (total > 0) {
            for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
                if ((double) entry.getValue() / total >= PREFERRED_GENRE_RATIO) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }
}
