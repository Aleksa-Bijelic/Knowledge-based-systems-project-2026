package com.example.bookstore.controller;

import com.example.bookstore.dto.OrderItemDiscountResponse;
import com.example.bookstore.dto.OrderItemRequest;
import com.example.bookstore.dto.OrderPreviewResponse;
import com.example.bookstore.dto.OrderRequest;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.rules.DiscountContext;
import com.example.bookstore.rules.OrderDiscountService;
import com.example.bookstore.service.BankPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final OrderDiscountService discountService;
    private final BankPaymentService bankPaymentService;

    public OrderController(BookRepository bookRepository, OrderRepository orderRepository,
                           OrderDiscountService discountService, BankPaymentService bankPaymentService) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.bankPaymentService = bankPaymentService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request,
                                          HttpServletRequest httpRequest) {
        try {
            Order order = buildOrderFromRequest(request);
            DiscountContext discountContext = discountService.evaluate(order);
            order.setDiscountAmount(discountContext.getChosenDiscount());
            order.setFinalAmount(order.getTotalAmount() - discountContext.getChosenDiscount());

            // Process card payment if payment method is "card"
            if ("card".equals(order.getPaymentMethod())) {
                if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
                    return ResponseEntity.badRequest().body("Card number is required for card payment");
                }
                if (request.getCardCvv() == null || request.getCardCvv().isBlank()) {
                    return ResponseEntity.badRequest().body("Card CVV is required for card payment");
                }
                if (request.getCardExpirationDate() == null || request.getCardExpirationDate().isBlank()) {
                    return ResponseEntity.badRequest().body("Card expiration date is required for card payment");
                }
                if (request.getCardholderName() == null || request.getCardholderName().isBlank()) {
                    return ResponseEntity.badRequest().body("Cardholder name is required for card payment");
                }

                String clientIp = request.getClientIp();
                if (clientIp == null || clientIp.isBlank()) {
                    clientIp = resolveClientIp(httpRequest);
                }
                Map<String, Object> paymentResult = bankPaymentService.processCardPayment(
                        request.getCardNumber(),
                        request.getCardCvv(),
                        request.getCardExpirationDate(),
                        request.getCardholderName(),
                        order.getFinalAmount(),
                        clientIp
                );

                Boolean success = (Boolean) paymentResult.get("success");
                if (success == null || !success) {
                    String message = (String) paymentResult.get("message");
                    return ResponseEntity.badRequest().body("Payment failed: " + message);
                }

                order.setStatus("COMPLETED");
            }

            Order saved = orderRepository.save(order);
            OrderPreviewResponse response = buildResponse(order, discountContext);
            return ResponseEntity.created(URI.create("/api/orders/" + saved.getId())).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewOrder(@RequestBody OrderRequest request) {
        try {
            Order order = buildOrderFromRequest(request);
            DiscountContext discountContext = discountService.evaluate(order);
            return ResponseEntity.ok(buildResponse(order, discountContext));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private Order buildOrderFromRequest(OrderRequest request) {
        if (request.getCustomerUsername() == null || request.getCustomerUsername().isEmpty()) {
            throw new IllegalArgumentException("Customer username is required");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }

            Book book = bookRepository.findById(itemRequest.getBookId()).orElse(null);
            if (book == null) {
                throw new IllegalArgumentException("Book with ID " + itemRequest.getBookId() + " not found");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setBook(book);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(book.getPrice());
            totalAmount += book.getPrice() * itemRequest.getQuantity();
            orderItems.add(orderItem);
        }

        Order order = new Order();
        order.setCustomerUsername(request.getCustomerUsername());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "cash");
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        return order;
    }

    private OrderPreviewResponse buildResponse(Order order, DiscountContext discountContext) {
        List<OrderItemDiscountResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemResponses.add(new OrderItemDiscountResponse(
                    item.getBook().getId(),
                    item.getBook().getTitle(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    discountContext.getItemDiscount(item)
            ));
        }

        return new OrderPreviewResponse(
                order.getTotalAmount(),
                discountContext.getChosenDiscount(),
                order.getTotalAmount() - discountContext.getChosenDiscount(),
                discountContext.getSelectedDiscountType(),
                itemResponses
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable String username) {
        List<Order> orders = orderRepository.findByCustomerUsername(username);
        return ResponseEntity.ok(orders);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

