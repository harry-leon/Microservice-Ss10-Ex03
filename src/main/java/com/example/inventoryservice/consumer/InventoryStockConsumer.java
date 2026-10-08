package com.example.inventoryservice.consumer;

import com.example.inventoryservice.entity.Medicine;
import com.example.inventoryservice.entity.Order;
import com.example.inventoryservice.event.OrderEvent;
import com.example.inventoryservice.repository.MedicineRepository;
import com.example.inventoryservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryStockConsumer {

    private final MedicineRepository medicineRepository;
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${app.kafka.topic.order-events:medicine-stock-events}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeOrderEvent(OrderEvent orderEvent) {
        log.info("Consumer đã nhận sự kiện: {}", orderEvent);

        Long medicineId = orderEvent.getMedicineId();
        Integer quantity = orderEvent.getQuantity();

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElse(null);

        Double price = medicine != null ? medicine.getPrice() : 0.0;
        Double totalAmount = (price != null ? price : 0.0) * (quantity != null ? quantity : 0);

        // 1. Thêm mới đơn hàng
        Order order = Order.builder()
                .medicineId(medicineId)
                .quantity(quantity)
                .priceSell(price)
                .totalAmount(totalAmount)
                .timestamp(orderEvent.getTimestamp() != null ? orderEvent.getTimestamp() : LocalDateTime.now())
                .build();
        orderRepository.save(order);

        // 2. Trừ stock của sản phẩm thuốc
        if (medicine != null) {
            int currentStock = medicine.getQuantity() != null ? medicine.getQuantity() : 0;
            int newStock = Math.max(0, currentStock - (quantity != null ? quantity : 0));
            medicine.setQuantity(newStock);
            medicineRepository.save(medicine);
            log.info("Đã cập nhật tồn kho thuốc ID: {}. Tồn kho trước: {}, bán: {}, tồn kho sau: {}",
                    medicineId, currentStock, quantity, newStock);
        } else {
            medicineRepository.deductStock(medicineId, quantity);
            log.warn("Không tìm thấy thông tin thuốc id: {} để tính giá, đã thực hiện deductStock.", medicineId);
        }

        System.out.println("Thêm mới đơn hàng thành công !");
    }
}
