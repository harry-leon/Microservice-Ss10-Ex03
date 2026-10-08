package com.example.inventoryservice;

import com.example.inventoryservice.entity.Medicine;
import com.example.inventoryservice.repository.MedicineRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(MedicineRepository medicineRepository) {
        return args -> {
            if (medicineRepository.findById(1L).isEmpty()) {
                Medicine medicine = Medicine.builder()
                        .medicineName("Paracetamol 500mg")
                        .description("Thuốc giảm đau hạ sốt")
                        .price(10000.0)
                        .quantity(100)
                        .build();
                medicineRepository.save(medicine);
            }
        };
    }
}
