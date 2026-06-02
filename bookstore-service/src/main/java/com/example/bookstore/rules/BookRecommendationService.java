package com.example.bookstore.rules;

import com.example.bookstore.model.AppUser;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class BookRecommendationService {

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

    public List<Book> getRecommendationsForAnonymousUser() {
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
    }

    @Transactional(readOnly = true)
    public List<Book> getRecommendationsForUser(String username) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return List.of();
        }

        UserRecommendationFact userFact = new UserRecommendationFact(username);
        userFact.setNewUser(false);
        userFact.setGenreSelectionMade(!user.getFavoriteGenres().isEmpty());
        userFact.setFavoriteGenres(user.getFavoriteGenres());

        List<Book> allBooks = bookRepository.findAll();
        List<Rating> allRatings = ratingRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();

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

            if (userFact.isNewUser()) {
                if (userFact.isUseUnauthenticatedRecommendation()) {
                    return getRecommendationsForAnonymousUser();
                }

                return kieSession.getObjects().stream()
                        .filter(o -> o instanceof BookScore)
                        .map(o -> (BookScore) o)
                        .filter(BookScore::isSelected)
                        .sorted(Comparator.comparing(BookScore::getAvgScore).reversed())
                        .map(bs -> findBook(allBooks, bs.getBookId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            return kieSession.getObjects().stream()
                    .filter(o -> o instanceof ExistingUserBookScore)
                    .map(o -> (ExistingUserBookScore) o)
                    .filter(ExistingUserBookScore::isSelected)
                    .sorted(Comparator.comparingInt(ExistingUserBookScore::getScore).reversed()
                            .thenComparing(ExistingUserBookScore::getPublishedDate,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(bs -> findBook(allBooks, bs.getBookId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } finally {
            kieSession.dispose();
        }
    }

    private Book findBook(List<Book> books, Long bookId) {
        return books.stream()
                .filter(b -> b.getId().equals(bookId))
                .findFirst()
                .orElse(null);
    }
}
