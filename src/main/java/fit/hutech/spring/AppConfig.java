package fit.hutech.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AppConfig - Cấu hình MVC
 * 
 * Lưu ý: KHÔNG đăng ký static resource handler /images/** ở đây.
 * Việc phục vụ ảnh được xử lý bởi ImageController để giải quyết vấn đề:
 * - File ảnh cũ (official books) nằm trong: uploads/
 * - File ảnh mới (author books) nằm trong: uploads/books/
 * - Nhưng cả hai đều có imagePath trong DB dạng: /images/books/filename
 *
 * ImageController sẽ tìm file theo tên file (phần cuối URL) trong tất cả thư mục.
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {
    // Static resource handling cho /images/** được delegate sang ImageController
}