package com.example.inventoryservice.controller;

import com.example.inventoryservice.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacySellController {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topic.order-events:medicine-stock-events}")
    private String topicName;

    @PostMapping("/sell")
    public ResponseEntity<String> sellMedicine(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("quantity") Integer quantity
    ) {
        String orderId = UUID.randomUUID().toString();
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .medicineId(medicineId)
                .quantity(quantity)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("Sending order event to Kafka topic='{}' with key='{}': {}", topicName, medicineId, event);
        kafkaTemplate.send(topicName, String.valueOf(medicineId), event);

        return ResponseEntity.ok("Thanh toán thành công! Sự kiện đã được gửi tới Kafka.");
    }
}
