package com.med.medStore.Controller;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;

import com.med.medStore.Services.ProductRepository;
import com.med.medStore.models.Product;
import com.med.medStore.models.ProductDto;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductRepository repo;

    @GetMapping({ "", "/" })
    public String showProductList(Model model) {
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("products", products);
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreate(Model model) {
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto", productDto);
        return "products/CreateProducts";
    }

    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result) {
        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "imageFile", "The image file is required"));
        }
        if (result.hasErrors()) {
            return "products/CreateProducts";
        }

        // save image
        MultipartFile image = productDto.getImageFile();
        Date createdAt = new Date();
        String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();
        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir, storageFileName), StandardCopyOption.REPLACE_EXISTING);

            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setBrand(productDto.getBrand());
        product.setPrice(productDto.getPrice());
        product.setCreatedAt(createdAt);
        product.setCategory(productDto.getCategory());
        product.setImageFileName(storageFileName);
        repo.save(product);

        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(Model model, @RequestParam int id) {

        try {
            Product product = repo.findById(id).get();
            model.addAttribute("product", product);

            ProductDto productDto = new ProductDto();
            productDto.setName(product.getName());
            productDto.setBrand(product.getBrand());
            productDto.setDescription(product.getDescription());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());

            model.addAttribute("productDto", productDto);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            return "redirect:/products";
        }

        return "products/EditProduct";
    }

    @PostMapping("/update")
    public String updateProduct(
            Model model,
            @RequestParam int id,
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result) {

        try {
            Product product = repo.findById(id).get();
            model.addAttribute("product", product);

            if (result.hasErrors()) {
                return "products/EditProduct";
            }

            // Update product with values from DTO
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setDescription(productDto.getDescription());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());

            // Handle image update only if a new image is provided
            if (productDto.getImageFile() != null && !productDto.getImageFile().isEmpty()) {
                String uploadDir = "public/images/";

                // delete old image
                if (product.getImageFileName() != null) {
                    Path oldImage = Paths.get(uploadDir + product.getImageFileName());
                    try {
                        Files.delete(oldImage);
                    } catch (Exception ex) {
                        System.out.println("Exception deleting old image: " + ex.getMessage());
                    }
                }

                // upload new image
                MultipartFile image = productDto.getImageFile();
                Date createdAt = new Date();
                String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

                try (InputStream inputStream = image.getInputStream()) {
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    Files.copy(inputStream, Paths.get(uploadDir, storageFileName),
                            StandardCopyOption.REPLACE_EXISTING);
                }

                product.setImageFileName(storageFileName);
            }

            // Save the updated product
            repo.save(product);
            return "redirect:/products";

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            result.addError(new FieldError("productDto", "general",
                    "Error updating product: " + ex.getMessage()));
            return "products/EditProduct";
        }
    }

    @GetMapping("/delete")
    public String deleteProduct(@RequestParam int id) {

        try {
            Product product = repo.findById(id).get();

            // delete image from public folder
            Path imagePath = Paths.get("public/images" + product.getImageFileName());
            try {
                Files.delete(imagePath);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
            
            // delete the product
            repo.delete(product);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return "redirect:/products";
    }

}
