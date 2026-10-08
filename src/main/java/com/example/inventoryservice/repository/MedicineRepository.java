package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    @Modifying
    @Query("UPDATE Medicine m SET m.quantity = m.quantity - :quantity WHERE m.id = :id")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
