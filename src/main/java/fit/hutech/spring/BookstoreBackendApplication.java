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

            // 3. Initializing Sample Books if Database is empty
            if (bookRepository.count() == 0) {
                fit.hutech.spring.entities.Category literature = categoryRepository.findByName("Văn học");
                fit.hutech.spring.entities.Category tech = categoryRepository.findByName("Lập Trình");
                fit.hutech.spring.entities.Category business = categoryRepository.findByName("Kinh doanh");
                fit.hutech.spring.entities.Category lifestyle = categoryRepository.findByName("Nghệ thuật");

                java.util.Random random = new java.util.Random();
                Object[][] sampleBooks = {
                        { "Dế Mèn Phiêu Lưu Ký", "Tô Hoài", 45000.0, literature,
                                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=400" },
                        { "Java Design Patterns", "Erich Gamma", 125000.0, tech,
                                "https://images.unsplash.com/photo-1587620962725-abab7fe55159?q=80&w=400" },
                        { "Rich Dad Poor Dad", "Robert Kiyosaki", 89000.0, business,
                                "https://images.unsplash.com/photo-1592492159418-39f319320569?q=80&w=400" },
                        { "The Art of War", "Sun Tzu", 55000.0, business,
                                "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=400" },
                        { "Clean Code", "Robert C. Martin", 150000.0, tech,
                                "https://images.unsplash.com/photo-1516116216624-53e697fedbea?q=80&w=400" },
                        { "Mắt Biếc", "Nguyễn Nhật Ánh", 65000.0, literature,
                                "https://images.unsplash.com/photo-1541963463532-d68292c34b19?q=80&w=400" },
                        { "Digital Minimalism", "Cal Newport", 95000.0, lifestyle,
                                "https://images.unsplash.com/photo-1491841573634-28140fc7ced7?q=80&w=400" },
                        { "Bố Già (The Godfather)", "Mario Puzo", 110000.0, literature,
                                "https://images.unsplash.com/photo-1531243269054-5ebf6f3ad0e6?q=80&w=400" },
                        { "Sapiens", "Yuval Noah Harari", 135000.0, business,
                                "https://images.unsplash.com/photo-1543004218-ee141104975e?q=80&w=400" },
                        { "Deep Work", "Cal Newport", 99000.0, lifestyle,
                                "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?q=80&w=400" }
                };

                for (Object[] b : sampleBooks) {
                    fit.hutech.spring.entities.Book book = new fit.hutech.spring.entities.Book();
                    book.setTitle((String) b[0]);
                    book.setAuthor((String) b[1]);
                    book.setPrice((Double) b[2]);
                    book.setCategory((fit.hutech.spring.entities.Category) b[3]);
                    book.setImagePath((String) b[4]);
                    book.setIsFeatured(true);
                    book.setIsOnSale(random.nextBoolean());
                    book.setDiscountPrice(book.getPrice() * 0.85);
                    book.setViewCount(random.nextInt(2000));
                    book.setTotalSold(0);
                    book.setQuantity(50);
                    book.setBookSource(random.nextBoolean() ? "OFFICIAL" : "AUTHOR");
                    bookService.addBook(book);
                }
                System.out.println("Seed data: Added " + sampleBooks.length + " sample books with Unsplash covers.");
            }
            
            // 🔥 QUAN TRỌNG: Đồng bộ số lượng bán từ hóa đơn thực tế trong MySQL
            // (Xóa bỏ hoàn toàn việc rót số liệu random tào lao)
            bookService.syncSoldCounts();
            
            // Đồng bộ lại author metadata cho chắc chắn
            bookService.syncAuthorsMetadata();
        };
    }
}
