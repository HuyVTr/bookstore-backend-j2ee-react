package fit.hutech.spring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom Image Controller để phục vụ file ảnh từ nhiều thư mục khác nhau.
 * 
 * Giải quyết vấn đề: các sách cũ (official) có file vật lý nằm trong uploads/
 * nhưng imagePath trong DB được lưu dạng /images/books/filename.jpg
 * 
 * Khi Spring's static resource handler nhận /images/books/filename.jpg, nó tìm
 * file "books/filename.jpg" trong thư mục gốc uploads/ -> không tìm thấy -> 500.
 * 
 * Controller này xử lý thủ công: lấy tên file cuối cùng và tìm trong tất cả
 * các thư mục uploads tiềm năng.
 */
@RestController
public class ImageController {

    // Danh sách tất cả thư mục có thể chứa file ảnh
    private static final String[] SEARCH_DIRS = {
        "VoTruongHuy_220801278/spring/uploads",
        "VoTruongHuy_220801278/spring/uploads/books",
        "spring/uploads",
        "spring/uploads/books",
        "uploads",
        "uploads/books",
        "../uploads",
        "../uploads/books"
    };

    /**
     * Xử lý request /images/books/{filename}
     * Tìm file trong tất cả thư mục uploads, kể cả thư mục gốc (cho sách cũ)
     */
    @GetMapping("/images/books/**")
    public ResponseEntity<Resource> serveBookImage(HttpServletRequest request) throws IOException {
        // Lấy tên file từ URL (phần cuối sau /images/books/)
        String requestPath = request.getRequestURI();
        String filename = requestPath.substring(requestPath.lastIndexOf('/') + 1);
        
        // Giải mã URL encoding (ví dụ: %20 -> space, %C3%A0 -> à)
        try {
            filename = java.net.URLDecoder.decode(filename, "UTF-8");
        } catch (Exception ignored) {}

        System.out.println("ImageController: Serving request for: " + requestPath + " -> filename: " + filename);

        // Tìm file trong tất cả thư mục
        for (String dir : SEARCH_DIRS) {
            Path filePath = Paths.get(dir, filename);
            System.out.println("  Checking: " + filePath.toAbsolutePath());
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                System.out.println("  FOUND: " + filePath.toAbsolutePath());
                Resource resource = new FileSystemResource(filePath.toFile());
                String contentType = determineContentType(filename);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                        .body(resource);
            }
        }

        System.out.println("  NOT FOUND in any directory for: " + filename);
        return ResponseEntity.notFound().build();
    }

    /**
     * Xử lý request /images/{filename} (path không có /books/)
     */
    @GetMapping("/images/**")
    public ResponseEntity<Resource> serveImage(HttpServletRequest request) throws IOException {
        String requestPath = request.getRequestURI();
        // Bỏ qua nếu path là /images/books/ (đã xử lý bởi method trên)
        // Nhưng Spring sẽ route /images/books/ đến method cụ thể hơn ở trên
        
        String filename = requestPath.substring(requestPath.lastIndexOf('/') + 1);
        try {
            filename = java.net.URLDecoder.decode(filename, "UTF-8");
        } catch (Exception ignored) {}

        System.out.println("ImageController (/images/**): Serving: " + filename);

        for (String dir : SEARCH_DIRS) {
            Path filePath = Paths.get(dir, filename);
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                Resource resource = new FileSystemResource(filePath.toFile());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, determineContentType(filename))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                        .body(resource);
            }
        }

        return ResponseEntity.notFound().build();
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
