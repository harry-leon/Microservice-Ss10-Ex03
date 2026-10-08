# [Bài tập giỏi 3] Triển khai Consumer cập nhật kho dược tự động

Dự án triển khai **Kafka Consumer** trong kiến trúc Microservice để tự động lắng nghe sự kiện đơn hàng thuốc từ Kafka topic và cập nhật tồn kho (Database) cũng như ghi nhận đơn hàng.

---

## 1. Mục tiêu
- **Kiến thức**: Cách cấu hình và sử dụng **Kafka Consumer** (Lesson 03 - Spring Kafka).
- **Kỹ năng**: Lắng nghe sự kiện từ Kafka (`@KafkaListener`), xử lý dữ liệu và thực thi nghiệp vụ tương ứng (Thêm mới đơn hàng, trừ số lượng tồn kho trong Database).

---

## 2. Mô tả chi tiết & Yêu cầu kỹ thuật
- **Bối cảnh**: Dịch vụ `inventory-service` cần tự động trừ số lượng tồn kho khi có sự kiện đơn hàng từ Kafka broker.
- **Yêu cầu kỹ thuật**:
  - Sử dụng annotation `@KafkaListener` để lắng nghe topic `medicine-stock-events`.
  - Khi nhận được `OrderEvent`, Consumer sẽ gọi xuống Database (PostgreSQL/MySQL) để thực hiện trừ số lượng tồn kho: `UPDATE medicines SET quantity = quantity - :quantity WHERE id = :id`.
  - Cấu hình `group-id: inventory-group` cho Consumer để đảm bảo tính nhất quán (không xử lý trùng lặp trong cùng một cụm).
  - Tự động thêm bản ghi đơn hàng mới vào bảng `orders` và trừ số lượng `quantity` của sản phẩm thuốc trong bảng `medicines`.

---

## 3. Cấu trúc thư mục

```
Ss10-Ex03/
├── docker-compose.yml              # Khởi chạy Zookeeper & Kafka Broker
├── pom.xml                         # Cấu hình Spring Boot, Kafka, JPA, PostgreSQL, Lombok
├── README.md                       # Hướng dẫn chi tiết
└── src/
    └── main/
        ├── java/com/example/inventoryservice/
        │   ├── InventoryServiceApplication.java
        │   ├── DataInitializer.java               # Khởi tạo dữ liệu mẫu thuốc
        │   ├── config/
        │   │   └── KafkaConfig.java               # Cấu hình Kafka Producer, ConsumerFactory, ListenerFactory
        │   ├── consumer/
        │   │   └── InventoryStockConsumer.java    # @KafkaListener xử lý lưu Order và trừ tồn kho
        │   ├── controller/
        │   │   └── PharmacySellController.java    # API POST /api/pharmacy/sell gửi sự kiện lên Kafka
        │   ├── entity/
        │   │   ├── Medicine.java                  # Entity bảng medicines (id, medicine_name, description, price, quantity)
        │   │   └── Order.java                     # Entity bảng orders (id, medicine_id, quantity, price_sell, total_amount, timestamp)
        │   ├── event/
        │   │   └── OrderEvent.java                # DTO sự kiện đơn hàng
        │   └── repository/
        │       ├── MedicineRepository.java        # Repository tương tác bảng medicines
        │       └── OrderRepository.java           # Repository tương tác bảng orders
        └── resources/
            └── application.yml                    # Cấu hình server, datasource, JPA, Kafka
```

---

## 4. Hướng dẫn cài đặt & Khởi chạy

### Bước 1: Khởi động Kafka Broker
Sử dụng file `docker-compose.yml`:
```bash
docker-compose up -d
```
Hoặc sử dụng cụm Kafka Broker đang chạy tại `localhost:9092`.

### Bước 2: Cấu hình Cơ sở dữ liệu (PostgreSQL)
Đảm bảo PostgreSQL đang chạy trên cổng `5432` với database `inventory_db`:
```sql
CREATE DATABASE inventory_db;
```
Ứng dụng sẽ tự động sinh các bảng `medicines` và `orders` nhờ cấu hình `ddl-auto: update`, đồng thời nạp dữ liệu thuốc mặc định (ID = 1, Paracetamol 500mg, quantity = 100).

### Bước 3: Build và Chạy ứng dụng
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

---

## 5. Kết quả mong muốn & Kiểm thử API

### 1. Gửi sự kiện bán thuốc lên Kafka
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/pharmacy/sell?medicineId=1&quantity=5`
- **Response**:
```text
Thanh toán thành công! Sự kiện đã được gửi tới Kafka.
```

### 2. Log xử lý của Kafka Consumer
Khi nhận được sự kiện từ Kafka topic `medicine-stock-events`, Consumer thực thi:
1. Thêm mới bản ghi vào bảng `orders` (insert into orders ...).
2. Tìm kiếm thuốc trong bảng `medicines` (select m1_0.id, ... from medicines m1_0 where m1_0.id = ?).
3. Cập nhật trừ tồn kho trong bảng `medicines` (update medicines set ... quantity = ? where id = ?).
4. In thông báo xác nhận:
```text
Thêm mới đơn hàng thành công !
```
