package com.example.bookstore.rules;

import com.example.bookstore.model.AppUser;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Rating;
import com.example.bookstore.repository.BookRepository;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class BookRecommendationService {

    private final BookRepository bookRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final KieContainer kieContainer;

    public BookRecommendationService(BookRepository bookRepository, RatingRepository ratingRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;

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

    public List<Book> getRecommendationsForUser(String username) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return List.of();
        }

        UserRecommendationFact userFact = new UserRecommendationFact(username);
        userFact.setNewUser(true);
        userFact.setGenreSelectionMade(!user.getFavoriteGenres().isEmpty());
        userFact.setFavoriteGenres(user.getFavoriteGenres());

        List<Book> allBooks = bookRepository.findAll();
        List<Rating> allRatings = ratingRepository.findAll();

        KieSession kieSession = kieContainer.newKieSession();
        try {
            for (Book book : allBooks) {
                kieSession.insert(book);
            }
            for (Rating rating : allRatings) {
                kieSession.insert(rating);
            }
            kieSession.insert(userFact);
            kieSession.fireAllRules();

            if (userFact.isUseUnauthenticatedRecommendation()) {
                return getRecommendationsForAnonymousUser();
            }

            return kieSession.getObjects().stream()
                    .filter(o -> o instanceof BookScore)
                    .map(o -> (BookScore) o)
                    .filter(BookScore::isSelected)
                    .sorted(Comparator.comparing(BookScore::getAvgScore).reversed())
                    .map(bs -> allBooks.stream()
                            .filter(b -> b.getId().equals(bs.getBookId()))
                            .findFirst()
                            .orElse(null))
                    .filter(b -> b != null)
                    .collect(Collectors.toList());
        } finally {
            kieSession.dispose();
        }
    }
}
