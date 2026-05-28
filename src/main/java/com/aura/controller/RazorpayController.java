package com.aura.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @PostMapping("/order")
    public Map<String, Object> createOrder(@RequestBody Map<String, String> body) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            String plan = body.get("plan");
            int amount = plan.equals("pro") ? 1999 : 999; // in rupees × 100 = paise

            JSONObject options = new JSONObject();
            options.put("amount", amount * 100); // paise
            options.put("currency", "INR");
            options.put("receipt", "order_" + System.currentTimeMillis());

            Order order = client.orders.create(options);
            return Map.of(
                "orderId", order.get("id"),
                "amount", amount * 100,
                "currency", "INR",
                "keyId", keyId
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}