package com.example.bookstore.controller;

import com.example.bookstore.dto.OrderItemRequest;
import com.example.bookstore.dto.OrderRequest;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    public OrderController(BookRepository bookRepository, OrderRepository orderRepository) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId()).orElse(null);
            if (book == null) {
                return ResponseEntity.badRequest().build();
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
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("CREATED");
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId())).body(saved);
    }
}
