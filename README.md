# 📚 Online Bookstore - Backend System (Spring Boot)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-JSON%20Web%20Token-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

Hệ thống Backend mạnh mẽ cho dự án Website Bán Sách Trực Tuyến, được xây dựng trên nền tảng Spring Boot với kiến trúc RESTful API, hỗ trợ đầy đủ các tính năng quản lý, bảo mật và tích hợp mạng xã hội.

## 🛠️ Công Nghệ Sử Dụng

- **Framework:** Spring Boot 3.4.3
- **Ngôn ngữ:** Java 21 (LTS)
- **Bảo mật:** Spring Security + JWT (JSON Web Token)
- **Cơ sở dữ liệu:** MySQL (JPA / Hibernate)
- **Xác thực Social:** OAuth2 Client (Google, Facebook, GitHub)
- **Công cụ hỗ trợ:** Lombok, MapStruct (tương lai), Apache POI (Excel Export), OpenPDF (PDF Export)
- **Build Tool:** Maven

## ✨ Tính Năng Chính

- **Quản lý người dùng & Bảo mật:**
  - Xác thực đa phương thức: Username/Email + Password hoặc qua Social Login (Google, Facebook, GitHub).
  - Phân quyền người dùng (RBAC): Admin, Staff, User, Guest.
  - Bảo mật phiên làm việc với JWT Token.
- **Quản lý kho sách (Inventory):**
  - Quản lý thông tin sách chi tiết (Mô tả, NXB, Năm, Kích thước, Số trang...).
  - Theo dõi tồn kho & Tự động giảm tồn kho khi có đơn hàng.
  - Quản lý danh mục sách với hệ thống Icon Mapping.
- **Hệ thống đơn hàng (Order System):**
  - Xử lý Checkout một phần (Partial Checkout) từ giỏ hàng.
  - Theo dõi trạng thái đơn hàng (Confirming, Shipping, Completed, Cancelled).
  - Ghi chú đơn hàng & Thông tin người gửi/ngận riêng biệt.
- **Báo cáo & Xuất dữ liệu:**
  - Xuất báo cáo doanh thu, tồn kho ra file Excel chuyên nghiệp.
  - Xuất hóa đơn/thông tin đơn hàng ra file PDF.
- **Xử lý Multimedia:**
  - Hệ thống Upload ảnh (Avatar, Book Covers) trực tiếp lên Server.

## 🚀 Hướng Dẫn Cài Đặt

### Tiền đề (Prerequisites)
- JDK 21 trở lên.
- MySQL Server 8.0+.
- Maven 3.x.

### Các bước thực hiện
1. **Clone repository:**
   ```bash
   git clone <link-repo-backend>
   ```
2. **Cấu hình Database:**
   - Tạo database trong MySQL: `CREATE DATABASE \`Project Bookstore CV\`;`
   - Cập nhật thông tin `username` và `password` trong file `src/main/resources/application.properties`.
3. **Chạy ứng dụng:**
   ```bash
   mvn spring-boot:run
   ```
   Ứng dụng sẽ khởi chạy tại port `8080` (mặc định).

## 📂 Cấu Trúc Thư Mục
```text
src/main/java/fit/hutech/spring/
├── config/         # Cấu hình Security, CORS, Cloudinary...
├── controllers/    # Xử lý REST Endpoints
├── dtos/           # Data Transfer Objects
├── entities/       # JPA Entities (Database Mapping)
├── repositories/   # Giao tiếp với Database
├── security/       # Xử lý JWT & Custom Auth logic
├── services/       # Xử lý logic nghiệp vụ (Business Logic)
└── utils/          # Các hàm tiện ích (Excel, PDF helper)
```

## 🔐 Biến Môi Trường (Cấu hình chính)
Các tham số quan quan trọng trong `application.properties`:
- `spring.datasource.url`: Đường dẫn kết nối MySQL.
- `app.jwtSecret`: Khóa bí mật dùng để ký JWT Token.
- `app.upload.dir`: Thư mục lưu trữ ảnh tải lên (mặc định: `uploads`).

