package com.signuplogin.demo.controllers;

import com.signuplogin.demo.entity.*;
import com.signuplogin.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/orders")
public class PaymentController {

    @Autowired
    private signuprepo signrepo;
    @Autowired
    private CartRepository cartRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private OrderItemRepository orderItemRepo;

    // ✅ Read env variables safely at runtime
    String clientId = System.getenv("CASHFREE_CLIENT_ID");
    String clientSecret = System.getenv("CASHFREE_CLIENT_SECRET");


    // -------------------- PLACE ORDER --------------------
    @PostMapping("/place/{userId}")
    public ResponseEntity<?> placeOrder(@PathVariable int userId) {
        Optional<Signup> optionalUser = signrepo.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "User not found"));
        }
        Signup user = optionalUser.get();

        List<Cart> cartItems = cartRepo.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Cart is empty"));
        }

        double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getNewPrice() * item.getQuantity())
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepo.save(order);

        for (Cart cart : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cart.getProduct());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(cart.getProduct().getNewPrice());
            orderItemRepo.save(orderItem);
        }

        return ResponseEntity.ok(savedOrder);
    }

    // -------------------- CREATE CASHFREE PAYMENT ORDER --------------------
    @PostMapping("/payment/{userId}/{orderId}")
    public ResponseEntity<Map<String, Object>> createCashfreeOrder(
            @PathVariable int userId,
            @PathVariable int orderId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String orderAmountStr = Objects.toString(request.get("orderAmount"), null);
            if (orderAmountStr == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Missing order amount"));
            }

            double orderAmount = Double.parseDouble(orderAmountStr);
            if (orderAmount <= 0) {
                return ResponseEntity.status(400).body(Map.of("error", "Invalid order amount"));
            }

            String customerEmail = Objects.toString(request.get("customerEmail"), "guest@example.com");
            String customerPhone = Objects.toString(request.get("customerPhone"), "9999999999");
            String customerName = Objects.toString(request.getOrDefault("customerName", "Guest User"));

            String cashfreeOrderId = String.valueOf(orderId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", cashfreeOrderId);
            payload.put("order_amount", orderAmount);
            payload.put("order_currency", "INR");

            Map<String, Object> orderMeta = new HashMap<>();
            orderMeta.put("return_url", "http://localhost:5173/payment-success?orderId=" + orderId);
            payload.put("order_meta", orderMeta);

            Map<String, String> customer = new HashMap<>();
            customer.put("customer_id", "CUST_" + userId);
            customer.put("customer_email", customerEmail);
            customer.put("customer_phone", customerPhone);
            customer.put("customer_name", customerName);
            payload.put("customer_details", customer);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("x-client-id", CLIENT_ID);
            headers.set("x-client-secret", CLIENT_SECRET);
            headers.set("x-api-version", "2022-09-01");

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://sandbox.cashfree.com/pg/orders",
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Invalid response from Cashfree"));
            }

            Map<String, Object> cfResponse = response.getBody();
            String paymentSessionId = (String) cfResponse.get("payment_session_id");

            if (paymentSessionId == null) {
                return ResponseEntity.status(500).body(Map.of("error", "No payment session returned"));
            }

            return ResponseEntity.ok(Map.of(
                    "paymentSessionId", paymentSessionId,
                    "orderId", cashfreeOrderId
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Exception: " + e.getMessage()));
        }
    }

    // -------------------- VERIFY PAYMENT --------------------
    @PostMapping("/verify/{orderId}")
    @Transactional
    public ResponseEntity<?> verifyPayment(
            @PathVariable int orderId,
            @RequestBody(required = false) Map<String, Object> payload) {
        try {
            if (payload == null) payload = new HashMap<>();

            String paymentId = (String) payload.getOrDefault("payment_id", "dummy_payment_id");
            String signature = (String) payload.getOrDefault("signature", "dummy_signature");

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            order.setStatus("PAID");
            orderRepo.save(order);

            Signup user = order.getUser();
            if (user != null) cartRepo.deleteByUser(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment verified successfully and cart cleared",
                    "orderId", orderId
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Payment verification failed: " + e.getMessage()
            ));
        }
    }

    // -------------------- PAYMENT SUCCESS HANDLER --------------------
    @PostMapping("/payment/success")
    @Transactional
    public ResponseEntity<?> handlePaymentSuccess(@RequestBody Map<String, Object> data) {
        try {
            int userId = ((Number) data.get("userId")).intValue();
            int orderId = ((Number) data.get("orderId")).intValue();

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            if (order.getUser().getId() != userId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Order does not belong to the given user"));
            }

            order.setStatus("PAID");
            orderRepo.save(order);

            List<Cart> userCart = cartRepo.findByUserId(userId);
            if (!userCart.isEmpty()) cartRepo.deleteAll(userCart);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment successful! Order confirmed and cart cleared.",
                    "orderId", order.getId(),
                    "status", order.getStatus()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }
}
