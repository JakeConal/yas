package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.ImageVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    private ProductDetailService productDetailService;

    @BeforeEach
    void setUp() {
        productDetailService = new ProductDetailService(
            productRepository,
            mediaService,
            productOptionCombinationRepository
        );
    }

    @Test
    void getProductDetailById_whenProductHasNoOptions_returnsMappedDetail() {
        Product product = createProduct(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        stubMedia(100L);
        stubMedia(101L);

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Product 1");
        assertThat(result.getBrandId()).isEqualTo(10L);
        assertThat(result.getBrandName()).isEqualTo("Acme");
        assertThat(result.getCategories()).extracting(Category::getName).containsExactly("Phones");
        assertThat(result.getAttributeValues()).hasSize(1);
        assertThat(result.getVariations()).isEmpty();
        assertThat(result.getThumbnail()).isEqualTo(new ImageVm(100L, "https://media/100"));
        assertThat(result.getProductImages()).containsExactly(new ImageVm(101L, "https://media/101"));
    }

    @Test
    void getProductDetailById_whenProductHasOptions_returnsPublishedVariations() {
        Product parentProduct = createProduct(true);
        Product variation = Product.builder()
            .id(2L)
            .name("Variant 1")
            .slug("variant-1")
            .sku("SKU-V1")
            .gtin("GTIN-V1")
            .price(19.99)
            .thumbnailMediaId(200L)
            .isPublished(true)
            .build();
        variation.getProductImages().add(ProductImage.builder().imageId(201L).product(variation).build());
        parentProduct.getProducts().add(variation);

        when(productRepository.findById(1L)).thenReturn(Optional.of(parentProduct));
        stubMedia(100L);
        stubMedia(101L);
        stubMedia(200L);
        stubMedia(201L);

        ProductOption productOption = new ProductOption();
        productOption.setId(10L);
        productOption.setName("Color");
        ProductOptionCombination combination = ProductOptionCombination.builder()
            .productOption(productOption)
            .value("Red")
            .build();
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getVariations()).hasSize(1);
        assertThat(result.getVariations().get(0).options()).containsEntry(10L, "Red");
        assertThat(result.getVariations().get(0).thumbnail()).isEqualTo(new ImageVm(200L, "https://media/200"));
        assertThat(result.getVariations().get(0).productImages())
            .containsExactly(new ImageVm(201L, "https://media/201"));
    }

    @Test
    void getProductDetailById_whenProductMissing_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(99L));
    }

    private void stubMedia(Long id) {
        when(mediaService.getMedia(id)).thenReturn(new com.yas.product.viewmodel.NoFileMediaVm(
            id,
            "caption-" + id,
            "file-" + id,
            "image/png",
            "https://media/" + id
        ));
    }

    private Product createProduct(boolean hasOptions) {
        Brand brand = new Brand();
        brand.setId(10L);
        brand.setName("Acme");
        brand.setSlug("acme");
        brand.setPublished(true);
        Category category = new Category();
        category.setId(20L);
        category.setName("Phones");
        category.setSlug("phones");

        ProductAttributeGroup attributeGroup = new ProductAttributeGroup();
        attributeGroup.setId(30L);
        attributeGroup.setName("Specs");

        ProductAttribute attribute = ProductAttribute.builder()
            .id(40L)
            .name("Memory")
            .productAttributeGroup(attributeGroup)
            .build();

        ProductAttributeValue attributeValue = new ProductAttributeValue();
        attributeValue.setId(50L);
        attributeValue.setProductAttribute(attribute);
        attributeValue.setValue("8 GB");

        Product product = Product.builder()
            .id(1L)
            .name("Product 1")
            .shortDescription("Short description")
            .description("Description")
            .specification("Specification")
            .sku("SKU-1")
            .gtin("GTIN-1")
            .slug("product-1")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(true)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .price(99.99)
            .thumbnailMediaId(100L)
            .taxClassId(7L)
            .hasOptions(hasOptions)
            .build();
        product.setBrand(brand);
        product.getProductCategories().add(ProductCategory.builder().product(product).category(category).build());
        product.getAttributeValues().add(attributeValue);
        product.getProductImages().add(ProductImage.builder().imageId(101L).product(product).build());
        return product;
    }
}