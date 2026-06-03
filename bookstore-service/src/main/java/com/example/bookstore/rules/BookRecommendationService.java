package com.example.bookstore.rules;

import com.example.bookstore.model.AppUser;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;
import com.example.bookstore.model.Rating;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.RatingRepository;
import com.example.bookstore.repository.UserRepository;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(BookRecommendationService.class);

    private static final int NEW_USER_RATING_THRESHOLD = 10;
    private static final int TOP_AUTHORS_FOR_NEW_USER = 4;
    private static final int TOP_BOOKS_FOR_NEW_USER = 10;
    private static final int TOP_BOOKS_FOR_EXISTING_USER = 20;

    private final BookRepository bookRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final KieContainer kieContainer;

    public BookRecommendationService(BookRepository bookRepository, RatingRepository ratingRepository,
                                     UserRepository userRepository, OrderRepository orderRepository) {
        this.bookRepository = bookRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/book-recommendation-rules.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/user-recommendation-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new IllegalStateException("Drools build errors: " + results.getMessages());
        }

        this.kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    @Transactional(readOnly = true)
    public List<Book> getRecommendationsForAnonymousUser() {
        try {
            Random random = new Random();

            List<BookRecommendationFact> facts = bookRepository.findAll().stream()
                    .map(book -> {
                        BookRecommendationFact fact = new BookRecommendationFact(
                                book,
                                book.getAddedAt(),
                                book.getPublishedDate(),
                                LocalDate.now()
                        );
                        fact.setRandomScore(random.nextDouble());
                        return fact;
                    })
                    .collect(Collectors.toList());

            List<Rating> ratings = ratingRepository.findAll();

            KieSession kieSession = kieContainer.newKieSession();
            try {
                for (BookRecommendationFact fact : facts) {
                    kieSession.insert(fact);
                }
                for (Rating rating : ratings) {
                    kieSession.insert(rating);
                }
                kieSession.fireAllRules();
            } finally {
                kieSession.dispose();
            }

            return facts.stream()
                    .filter(BookRecommendationFact::isRecommended)
                    .map(BookRecommendationFact::getBook)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting anonymous recommendations", e);
            return Collections.emptyList();
        }
    }

    private void forceLoadLazyData(List<Order> orders, List<Rating> ratings) {
        if (orders != null) {
            for (Order order : orders) {
                if (order != null && order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        if (item != null && item.getBook() != null) {
                            item.getBook().getTitle();
                        }
                    }
                }
            }
        }
        if (ratings != null) {
            for (Rating rating : ratings) {
                if (rating != null && rating.getBook() != null) {
                    rating.getBook().getTitle();
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Book> getRecommendationsForUser(String username) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", username);
            return List.of();
        }

        try {
            long ratingCount = ratingRepository.countByUsername(username);
            boolean isNewUser = ratingCount < NEW_USER_RATING_THRESHOLD;
            boolean genreSelectionMade = user.getFavoriteGenres() != null && !user.getFavoriteGenres().isEmpty();

            log.info("User: {} | ratings: {} | newUser: {} | genreSelectionMade: {} | genres: {}",
                    username, ratingCount, isNewUser, genreSelectionMade, user.getFavoriteGenres());

            UserRecommendationFact userFact = new UserRecommendationFact(username);
            userFact.setNewUser(isNewUser);
            userFact.setGenreSelectionMade(genreSelectionMade);
            userFact.setFavoriteGenres(user.getFavoriteGenres());

            List<Book> allBooks = bookRepository.findAll();
            List<Rating> allRatings = ratingRepository.findAll();
            List<Order> allOrders = orderRepository.findAll();

            forceLoadLazyData(allOrders, allRatings);

            KieSession kieSession = kieContainer.newKieSession();
            try {
                for (Book book : allBooks) {
                    kieSession.insert(book);
                }
                for (Rating rating : allRatings) {
                    kieSession.insert(rating);
                }
                for (Order order : allOrders) {
                    kieSession.insert(order);
                }
                kieSession.insert(userFact);
                kieSession.fireAllRules();

                if (isNewUser) {
                    if (!genreSelectionMade) {
                        log.info("New user {} without genre selection -> anonymous recommendations", username);
                        return getRecommendationsForAnonymousUser();
                    }
                    return selectTopBooksForNewUser(kieSession, allBooks);
                }

                return selectTopBooksForExistingUser(kieSession, allBooks);
            } finally {
                kieSession.dispose();
            }
        } catch (Exception e) {
            log.error("Error getting recommendations for user: {}", username, e);
            return Collections.emptyList();
        }
    }

    private List<Book> selectTopBooksForNewUser(KieSession kieSession, List<Book> allBooks) {
        List<QualifiedAuthor> qualifiedAuthors = kieSession.getObjects().stream()
                .filter(o -> o instanceof QualifiedAuthor)
                .map(o -> (QualifiedAuthor) o)
                .collect(Collectors.toList());

        log.debug("Qualified authors count: {}", qualifiedAuthors.size());

        Set<String> topAuthorNames = qualifiedAuthors.stream()
                .sorted(Comparator.comparingLong(QualifiedAuthor::getTotalRatingCount).reversed())
                .limit(TOP_AUTHORS_FOR_NEW_USER)
                .map(QualifiedAuthor::getAuthor)
                .collect(Collectors.toCollection(HashSet::new));

        log.debug("Top {} authors: {}", TOP_AUTHORS_FOR_NEW_USER, topAuthorNames);

        List<BookScore> allBookScores = kieSession.getObjects().stream()
                .filter(o -> o instanceof BookScore)
                .map(o -> (BookScore) o)
                .collect(Collectors.toList());

        log.debug("Total BookScore entries: {}", allBookScores.size());

        return allBookScores.stream()
                .filter(bs -> {
                    Book book = findBook(allBooks, bs.getBookId());
                    return book != null && topAuthorNames.contains(book.getAuthor());
                })
                .sorted(Comparator.comparingDouble(BookScore::getAvgScore).reversed())
                .limit(TOP_BOOKS_FOR_NEW_USER)
                .map(bs -> findBook(allBooks, bs.getBookId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Book> selectTopBooksForExistingUser(KieSession kieSession, List<Book> allBooks) {
        List<ExistingUserBookScore> allScores = kieSession.getObjects().stream()
                .filter(o -> o instanceof ExistingUserBookScore)
                .map(o -> (ExistingUserBookScore) o)
                .collect(Collectors.toList());

        long similarUserCount = kieSession.getObjects().stream().filter(o -> o instanceof SimilarUser).count();
        long bookPairCount = kieSession.getObjects().stream().filter(o -> o instanceof BookPairSimilarity).count();
        long likedBookCount = kieSession.getObjects().stream().filter(o -> o instanceof LikedBookByUser).count();
        long prefAuthorCount = kieSession.getObjects().stream().filter(o -> o instanceof UserPreferredAuthor).count();
        long prefGenreCount = kieSession.getObjects().stream().filter(o -> o instanceof UserPreferredGenre).count();

        log.info("ExistingUserBookScore count: {} | similarUsers: {} | bookPairs: {} | likedBooks: {} | prefAuthors: {} | prefGenres: {}",
                allScores.size(), similarUserCount, bookPairCount, likedBookCount, prefAuthorCount, prefGenreCount);

        Map<Integer, Long> scoreDistribution = allScores.stream()
                .collect(Collectors.groupingBy(ExistingUserBookScore::getScore, Collectors.counting()));
        log.info("Score distribution: {}", scoreDistribution);

        List<ExistingUserBookScore> topScored = allScores.stream()
                .filter(s -> s.getScore() > 0)
                .sorted(Comparator
                        .comparingInt(ExistingUserBookScore::getScore).reversed()
                        .thenComparing(ExistingUserBookScore::getPublishedDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
        for (ExistingUserBookScore s : topScored) {
            log.info("  Top book: id={} title='{}' score={} c1={} c2={} c3={}",
                    s.getBookId(), s.getBookTitle(), s.getScore(),
                    s.isCriterion1Met(), s.isCriterion2Met(), s.isCriterion3Met());
        }

        return allScores.stream()
                .sorted(Comparator
                        .comparingInt(ExistingUserBookScore::getScore).reversed()
                        .thenComparing(ExistingUserBookScore::getPublishedDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TOP_BOOKS_FOR_EXISTING_USER)
                .map(bs -> findBook(allBooks, bs.getBookId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Book findBook(List<Book> books, Long bookId) {
        if (bookId == null || books == null) return null;
        return books.stream()
                .filter(b -> b != null && b.getId() != null && b.getId().equals(bookId))
                .findFirst()
                .orElse(null);
    }
}
