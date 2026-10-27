package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.catalog.ProductResponse;
import com.faust0z.B2BSystem.catalog.application.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/catalog/products")
@Tag(name = "Products", description = "Product catalog management")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all products")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping
    List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @Operation(summary = "Create a product")
    @ApiResponse(responseCode = "201", description = "Product created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    @Operation(summary = "Update a product")
    @ApiResponse(responseCode = "200", description = "Product updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PatchMapping("/{productId}")
    ProductResponse updateProduct(@PathVariable UUID productId, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(productId, request);
    }

    @Operation(summary = "Update a product status -> active/inactive")
    @ApiResponse(responseCode = "200", description = "Product status updated successfully")
    @PatchMapping("/{productId}/status")
    ProductResponse toggleProductStatus(@PathVariable UUID productId) {
        return productService.toggleProductStatus(productId);
    }
}
