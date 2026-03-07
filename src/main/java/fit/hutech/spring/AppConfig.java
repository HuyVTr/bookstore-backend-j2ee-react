package fit.hutech.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mảng các đường dẫn tiềm năng
        java.util.List<String> locations = new java.util.ArrayList<>();

        try {
            // 1. Thử tìm trong thư mục VoTruongHuy_220801278/spring/uploads (Trường hợp
            // khởi chạy từ root DoAn/J2EE/Update more)
            java.nio.file.Path path = java.nio.file.Paths.get("VoTruongHuy_220801278/spring/uploads");

            if (!java.nio.file.Files.exists(path)) {
                // Background fallback: thử các đường dẫn cũ
                path = java.nio.file.Paths.get("spring/uploads");
            }
            if (!java.nio.file.Files.exists(path)) {
                path = java.nio.file.Paths.get("uploads");
            }

            // Nếu tìm thấy thư mục tồn tại
            if (java.nio.file.Files.exists(path)) {
                String absolutePath = path.toAbsolutePath().toUri().toString();
                if (!absolutePath.endsWith("/")) {
                    absolutePath += "/";
                }
                System.out
                        .println("=============== THƯ MỤC ẢNH ĐƯỢC TÌM THẤY TẠI: " + absolutePath + " ===============");
                locations.add(absolutePath);
            }
        } catch (Exception e) {
            System.err.println("Error resolving absolute path: " + e.getMessage());
        }

        // 2. Thêm các đường dẫn fallback
        locations.add("file:VoTruongHuy_220801278/spring/uploads/");
        locations.add("file:spring/uploads/");
        locations.add("file:uploads/");

        registry.addResourceHandler("/images/books/**")
                .addResourceLocations(locations.toArray(new String[0]));
    }
}