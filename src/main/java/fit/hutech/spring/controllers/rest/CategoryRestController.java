package fit.hutech.spring.controllers.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.entities.Category;
import fit.hutech.spring.repositories.ICategoryRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/categories")
@RequiredArgsConstructor
public class CategoryRestController {

    // In a real application, logic should be in CategoryService.
    // For simplicity following the existing codebase pattern, using Context
    // directly.
    private final ICategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.status(201).body(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryUpdates) {
        return categoryRepository.findById(id).map(existingCategory -> {
            existingCategory.setName(categoryUpdates.getName());
            Category updatedCategory = categoryRepository.save(existingCategory);
            return ResponseEntity.ok(updatedCategory);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        return categoryRepository.findById(id).map(existingCategory -> {
            categoryRepository.delete(existingCategory);
            return ResponseEntity.ok("Deleted successfully");
        }).orElse(ResponseEntity.notFound().build());
    }
}
