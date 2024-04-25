package com.rrss.backend.service;

import com.rrss.backend.dto.AddProductRequest;
import com.rrss.backend.dto.ProductDto;
import com.rrss.backend.dto.UpdateProductRequest;
import com.rrss.backend.model.Product;
import com.rrss.backend.model.User;
import com.rrss.backend.repository.ProductCategoryRepository;
import com.rrss.backend.repository.ProductRepository;
import com.rrss.backend.util.ImageUtil;
import com.rrss.backend.util.UserUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.zip.DataFormatException;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductCategoryService productCategoryService;
    private final UserUtil userUtil;
    private final ProductCategoryRepository productCategoryRepository;

    public ProductService(ProductRepository repository, ProductCategoryService productCategoryService, UserUtil userUtil, ProductCategoryRepository productCategoryRepository) {
        this.repository = repository;
        this.productCategoryService = productCategoryService;
        this.userUtil = userUtil;
        this.productCategoryRepository = productCategoryRepository;
    }

    public ProductDto addProduct(Principal currentUser, AddProductRequest addProductRequest, MultipartFile file) throws IOException {

        User user = userUtil.extractUser(currentUser);

        Product product = new Product(
                addProductRequest.name(),
                addProductRequest.description(),
                user.getMerchant(),
                productCategoryService.findByName(addProductRequest.productCategoryName()),
                addProductRequest.price(),
                ImageUtil.compressImage(file.getBytes())
        );

        return ProductDto.convert(repository.save(product));
    }

    public ProductDto deleteProduct(Principal currentUser, Long productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        User user = userUtil.extractUser(currentUser);

        if (user.getMerchant() == null || Objects.equals(user.getMerchant().getId(), product.getMerchant().getId())) {
            throw new IllegalArgumentException("you are not the owner of this product");
        }

        repository.delete(product);

        return ProductDto.convert(product);
    }

    public ProductDto getProduct(Long productId) {
        return ProductDto.convert(repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found")));
    }


    public byte[] downloadProductPicture(Long productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        try {
            return ImageUtil.decompressImage(product.getPicture());
        } catch (DataFormatException | IOException e) {
            throw new RuntimeException("change this....");
        }

    }

    public List<ProductDto> getAllProducts() {
        return repository
                .findAll()
                .stream()
                .map(ProductDto::convert)
                .toList();
    }

    public List<ProductDto> getProductsByCategory(String categoryName) {
        return repository
                .findByProductCategoryName(categoryName)
                .stream()
                .map(ProductDto::convert)
                .toList();
    }

    public ProductDto updateProduct(Principal currentUser, Long productId, UpdateProductRequest updateProductRequest) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        User user = userUtil.extractUser(currentUser);
        if (user.getMerchant() == null || Objects.equals(user.getMerchant().getId(), product.getMerchant().getId())) {
            throw new IllegalArgumentException("you are not the owner of this product");
        }

                new Product(
                productId,
                updateProductRequest.name(),
                updateProductRequest.description(),
                product.getMerchant(),
                productCategoryRepository.findByName(updateProductRequest.productCategoryName())
                        .orElseThrow(() -> new IllegalArgumentException("Product category not found")),
                updateProductRequest.price(),
                product.getPicture()
        );

        return ProductDto.convert(repository.save(product));
    }
}
