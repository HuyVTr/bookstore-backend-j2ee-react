package fit.hutech.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookstoreBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreBackendApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    org.springframework.boot.CommandLineRunner init(
            fit.hutech.spring.repositories.IRoleRepository roleRepository,
            fit.hutech.spring.repositories.ICategoryRepository categoryRepository,
            fit.hutech.spring.repositories.IBookRepository bookRepository,
            fit.hutech.spring.repositories.IAuthorRepository authorRepository,
            fit.hutech.spring.services.BookService bookService) {
        return args -> {
            // Initializing Roles
            if (roleRepository.findByName("ADMIN") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "ADMIN", null, null));
            }
            if (roleRepository.findByName("USER") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "USER", null, null));
            }
            if (roleRepository.findByName("STAFF") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "STAFF", null, null));
            }
            if (roleRepository.findByName("AUTHOR") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "AUTHOR", null, null));
            }

            // === DATABASE MIGRATION: RESTRUCTURING CATEGORIES ===
            // Chạy logic di cư thông qua Service để đảm bảo có Transaction
            try {
                bookService.migrateCategories(categoryRepository);
                bookService.syncAuthorsMetadata(); // Đồng bộ tác giả hiện có vào metadata table
            } catch (Exception e) {
                System.err.println("Database migration failed: " + e.getMessage());
            }

            // Initializing Remaining Category Icons (Icon Database concept)
            String[][] commonCategories = {
                    { "Văn học", "📚" }, { "Kinh doanh", "💼" }, { "Kỹ năng", "💡" }, { "Thiếu nhi", "🎈" },
                    { "Ngoại ngữ", "🌐" }, { "Lịch sử", "🏛️" }, { "Khoa học", "🔬" }, { "Tiểu thuyết", "📖" },
                    { "Sức khỏe", "🏥" }, { "Nấu ăn", "🍳" }, { "Nghệ thuật", "🎨" }, { "Thể thao", "⚽" },
                    { "Tâm lý", "🧠" }, { "Kinh tế", "📉" }, { "Pháp luật", "⚖️" }
            };

            // 1. Pre-create if missing
            for (String[] catInfo : commonCategories) {
                if (categoryRepository.findByName(catInfo[0]) == null) {
                    categoryRepository
                            .save(new fit.hutech.spring.entities.Category(null, catInfo[0], catInfo[1], null));
                }
            }

            // 2. Update existing icons based on name keywords
            java.util.List<fit.hutech.spring.entities.Category> categories = categoryRepository.findAll();
            for (fit.hutech.spring.entities.Category cat : categories) {
                if (cat.getIcon() == null || cat.getIcon().isEmpty()) {
                    String name = cat.getName().toLowerCase();
                    if (name.contains("văn học") || name.contains("truyện") || name.contains("thơ"))
                        cat.setIcon("📚");
                    else if (name.contains("kinh doanh") || name.contains("đầu tư") || name.contains("tài chính"))
                        cat.setIcon("�");
                    else if (name.contains("kỹ năng") || name.contains("phát triển"))
                        cat.setIcon("💡");
                    else if (name.contains("thiếu nhi") || name.contains("trẻ em"))
                        cat.setIcon("🎈");
                    else if (name.contains("lập trình") || name.contains("công nghệ") || name.contains("phần mềm"))
                        cat.setIcon("�");
                    else if (name.contains("game") || name.contains("trò chơi"))
                        cat.setIcon("🎮");
                    else if (name.contains("ngoại ngữ") || name.contains("tiếng"))
                        cat.setIcon("🌐");
                    else if (name.contains("lịch sử") || name.contains("địa lý") || name.contains("văn hóa"))
                        cat.setIcon("🏛️");
                    else if (name.contains("khoa học") || name.contains("vật lý") || name.contains("hóa học"))
                        cat.setIcon("🔬");
                    else if (name.contains("sức khỏe") || name.contains("y tế") || name.contains("y học"))
                        cat.setIcon("�");
                    else if (name.contains("nấu ăn") || name.contains("ẩm thực") || name.contains("bếp"))
                        cat.setIcon("🍳");
                    else if (name.contains("nghệ thuật") || name.contains("vẽ") || name.contains("âm nhạc"))
                        cat.setIcon("🎨");
                    else if (name.contains("thể thao") || name.contains("bóng đá") || name.contains("gym"))
                        cat.setIcon("⚽");
                    else if (name.contains("tâm lý"))
                        cat.setIcon("🧠");
                    else if (name.contains("kinh tế"))
                        cat.setIcon("📉");
                    else if (name.contains("pháp luật") || name.contains("luật"))
                        cat.setIcon("⚖️");
                    else
                        cat.setIcon("📖");
                    categoryRepository.save(cat);
                }
            }
        };
    }
}
