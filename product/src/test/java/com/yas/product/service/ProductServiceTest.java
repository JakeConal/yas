package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductRelated;
import com.yas.product.model.enumeration.FilterExistInWhSelection;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.ImageVm;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductCheckoutListVm;
import com.yas.product.viewmodel.product.ProductDetailGetVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductExportingDetailVm;
import com.yas.product.viewmodel.product.ProductFeatureGetVm;
import com.yas.product.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductInfoVm;
import com.yas.product.viewmodel.product.ProductListGetFromCategoryVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductVariationGetVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import com.yas.product.viewmodel.productattribute.ProductAttributeGroupGetVm;
import com.yas.product.viewmodel.productattribute.ProductAttributeValueVm;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private ProductOptionValueRepository productOptionValueRepository;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @Mock
    private ProductRelatedRepository productRelatedRepository;

    private ProductService productService;

    private Brand brand1;
    private Brand brand2;
    private Category category1;
    private Category category2;
    private List<Product> catalogProducts;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
            productRepository,
            mediaService,
            brandRepository,
            productCategoryRepository,
            categoryRepository,
            productImageRepository,
            productOptionRepository,
            productOptionValueRepository,
            productOptionCombinationRepository,
            productRelatedRepository
        );
        lenient().when(mediaService.getMedia(anyLong())).thenAnswer(invocation -> media((Long) invocation.getArgument(0)));
        initFixtures();
    }

    @Test
    void getLatestProducts_returnsEmptyWhenCountIsNotPositive() {
        assertThat(productService.getLatestProducts(0)).isEmpty();
        assertThat(productService.getLatestProducts(-1)).isEmpty();
    }

    @Test
    void getLatestProducts_returnsEmptyWhenRepositoryHasNoProducts() {
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of());

        assertThat(productService.getLatestProducts(5)).isEmpty();
    }

    @Test
    void getLatestProducts_returnsMappedProducts() {
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(catalogProducts.subList(0, 3));

        List<ProductListVm> result = productService.getLatestProducts(3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("Product 1");
        assertThat(result.get(0).slug()).isEqualTo("product-1");
    }

    @Test
    void getProductsWithFilter_returnsPagedContent() {
        Page<Product> productPage = new PageImpl<>(catalogProducts.subList(0, 2), PageRequest.of(0, 2), 2);
        when(productRepository.getProductsWithFilter("product", "Brand One", PageRequest.of(0, 2)))
            .thenReturn(productPage);

        ProductListGetVm result = productService.getProductsWithFilter(0, 2, " Product ", "Brand One ");

        assertThat(result.productContent()).hasSize(2);
        assertThat(result.pageNo()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getProductsByBrand_returnsMappedThumbnails() {
        when(brandRepository.findBySlug(brand1.getSlug())).thenReturn(Optional.of(brand1));
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand1)).thenReturn(catalogProducts.subList(0, 2));

        List<ProductThumbnailVm> result = productService.getProductsByBrand(brand1.getSlug());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("https://media/101");
    }

    @Test
    void getProductsByBrand_throwsWhenBrandMissing() {
        when(brandRepository.findBySlug("missing-brand")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsByBrand("missing-brand"));
    }

    @Test
    void getProductsFromCategory_returnsPagedThumbnails() {
        Page<ProductCategory> productCategoryPage = new PageImpl<>(
            List.of(
                ProductCategory.builder().product(catalogProducts.get(0)).category(category1).build(),
                ProductCategory.builder().product(catalogProducts.get(1)).category(category1).build()
            ),
            PageRequest.of(0, 2),
            2
        );
        when(categoryRepository.findBySlug(category1.getSlug())).thenReturn(Optional.of(category1));
        when(productCategoryRepository.findAllByCategory(PageRequest.of(0, 2), category1)).thenReturn(productCategoryPage);

        ProductListGetFromCategoryVm result = productService.getProductsFromCategory(0, 2, category1.getSlug());

        assertThat(result.productContent()).hasSize(2);
        assertThat(result.productContent().get(0).thumbnailUrl()).isEqualTo("https://media/101");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void getProductsFromCategory_throwsWhenCategoryMissing() {
        when(categoryRepository.findBySlug("missing-category")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsFromCategory(0, 2, "missing-category"));
    }

    @Test
    void getProductById_returnsDetailVm() {
        Product product = catalogProducts.get(0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.brandId()).isEqualTo(1L);
        assertThat(result.categories()).extracting(Category::getName).containsExactly("Category One");
        assertThat(result.thumbnailMedia()).isEqualTo(new ImageVm(101L, "https://media/101"));
        assertThat(result.productImageMedias()).containsExactly(new ImageVm(201L, "https://media/201"));
    }

    @Test
    void getProductDetail_returnsGroupedAttributesAndImages() {
        Product product = detailProduct();
        when(productRepository.findBySlugAndIsPublishedTrue(product.getSlug())).thenReturn(Optional.of(product));

        ProductDetailGetVm result = productService.getProductDetail(product.getSlug());

        assertThat(result.id()).isEqualTo(product.getId());
        assertThat(result.brandName()).isEqualTo("Brand One");
        assertThat(result.productCategories()).containsExactly("Category One");
        assertThat(result.productAttributeGroups()).hasSize(2);
        assertThat(result.productAttributeGroups().get(0)).isEqualTo(
            new ProductAttributeGroupGetVm("Specs", List.of(new ProductAttributeValueVm("Memory", "8 GB")))
        );
        assertThat(result.productAttributeGroups().get(1).name()).isEqualTo("None group");
        assertThat(result.thumbnailMediaUrl()).isEqualTo("https://media/301");
        assertThat(result.productImageMediaUrls()).containsExactly("https://media/302", "https://media/303");
    }

    @Test
    void getFeaturedProductsById_fallsBackToParentThumbnailWhenNeeded() {
        Product parent = product(99L, "Parent", "parent", brand1, category1, 901L, 999.0, true);
        Product child = product(100L, "Child", "child", brand1, category1, null, 499.0, true);
        child.setParent(parent);
        when(productRepository.findAllByIdIn(List.of(100L))).thenReturn(List.of(child));
        when(productRepository.findById(99L)).thenReturn(Optional.of(parent));
        when(mediaService.getMedia((Long) isNull())).thenReturn(emptyMedia());

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(100L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().thumbnailUrl()).isEqualTo("https://media/901");
    }

    @Test
    void getListFeaturedProducts_returnsFeatureVm() {
        Page<Product> productPage = new PageImpl<>(catalogProducts.subList(0, 2), PageRequest.of(0, 2), 2);
        when(productRepository.getFeaturedProduct(PageRequest.of(0, 2))).thenReturn(productPage);

        ProductFeatureGetVm result = productService.getListFeaturedProducts(0, 2);

        assertThat(result.totalPage()).isEqualTo(1);
        assertThat(result.productList()).hasSize(2);
    }

    @Test
    void getProductsByMultiQuery_returnsPagedContent() {
        Page<Product> productPage = new PageImpl<>(catalogProducts.subList(1, 2), PageRequest.of(0, 2), 1);
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
            "product 2".trim().toLowerCase(),
            category2.getSlug(),
            1.0,
            20.0,
            PageRequest.of(0, 2)
        )).thenReturn(productPage);

        ProductsGetVm result = productService.getProductsByMultiQuery(0, 2, "Product 2", category2.getSlug(), 1.0, 20.0);

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().getFirst().name()).isEqualTo("Product 2");
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getProductVariationsByParentId_returnsPublishedVariations() {
        Product parent = product(200L, "Parent", "parent", brand1, category1, 700L, 999.0, true);
        parent.setHasOptions(true);
        Product publishedVariation = product(201L, "Variant 1", "variant-1", brand1, category1, 701L, 899.0, true);
        publishedVariation.getProductImages().add(ProductImage.builder().imageId(702L).product(publishedVariation).build());
        Product unpublishedVariation = product(202L, "Variant 2", "variant-2", brand1, category1, 703L, 799.0, false);
        unpublishedVariation.getProductImages().add(ProductImage.builder().imageId(704L).product(unpublishedVariation).build());
        parent.getProducts().addAll(List.of(publishedVariation, unpublishedVariation));
        when(productRepository.findById(200L)).thenReturn(Optional.of(parent));
        when(productOptionCombinationRepository.findAllByProduct(publishedVariation)).thenReturn(List.of(
            ProductOptionCombination.builder()
                .productOption(productOption(11L, "Color"))
                .value("Red")
                .build()
        ));

        List<ProductVariationGetVm> result = productService.getProductVariationsByParentId(200L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().options()).containsEntry(11L, "Red");
        assertThat(result.getFirst().productImages()).containsExactly(new ImageVm(702L, "https://media/702"));
    }

    @Test
    void getProductVariationsByParentId_returnsEmptyWhenParentHasNoOptions() {
        Product parent = product(201L, "Parent", "parent-no-options", brand1, category1, 800L, 999.0, true);
        parent.setHasOptions(false);
        when(productRepository.findById(201L)).thenReturn(Optional.of(parent));

        assertThat(productService.getProductVariationsByParentId(201L)).isEmpty();
    }

    @Test
    void deleteProduct_softDeletesVariantAndRemovesOptionCombinations() {
        Product parent = product(300L, "Parent", "parent-delete", brand1, category1, 810L, 999.0, true);
        Product variant = product(301L, "Variant", "variant-delete", brand1, category1, 811L, 899.0, true);
        variant.setParent(parent);
        ProductOptionCombination combination = ProductOptionCombination.builder()
            .product(variant)
            .productOption(productOption(12L, "Size"))
            .value("L")
            .build();
        when(productRepository.findById(301L)).thenReturn(Optional.of(variant));
        when(productOptionCombinationRepository.findAllByProduct(variant)).thenReturn(List.of(combination));
        when(productRepository.save(variant)).thenReturn(variant);

        productService.deleteProduct(301L);

        assertThat(variant.isPublished()).isFalse();
        verify(productOptionCombinationRepository, times(1)).deleteAll(List.of(combination));
    }

    @Test
    void deleteProduct_softDeletesParentWithoutOptionCleanup() {
        Product product = product(302L, "Parent", "parent-no-variant", brand1, category1, 812L, 999.0, true);
        when(productRepository.findById(302L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        productService.deleteProduct(302L);

        assertThat(product.isPublished()).isFalse();
    }

    @Test
    void getProductSlug_returnsParentSlugForVariantAndOwnSlugForParent() {
        Product parent = product(400L, "Parent", "parent-slug", brand1, category1, 901L, 999.0, true);
        Product variant = product(401L, "Variant", "variant-slug", brand1, category1, 902L, 899.0, true);
        variant.setParent(parent);
        when(productRepository.findById(401L)).thenReturn(Optional.of(variant));
        when(productRepository.findById(400L)).thenReturn(Optional.of(parent));

        ProductSlugGetVm variantSlug = productService.getProductSlug(401L);
        ProductSlugGetVm parentSlug = productService.getProductSlug(400L);

        assertThat(variantSlug.slug()).isEqualTo("parent-slug");
        assertThat(variantSlug.productVariantId()).isEqualTo(401L);
        assertThat(parentSlug.slug()).isEqualTo("parent-slug");
        assertThat(parentSlug.productVariantId()).isNull();
    }

    @Test
    void getProductEsDetailById_returnsDetailVm() {
        Product product = detailProduct();
        when(productRepository.findById(500L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(500L);

        assertThat(result.id()).isEqualTo(500L);
        assertThat(result.brand()).isEqualTo("Brand One");
        assertThat(result.categories()).containsExactly("Category One");
        assertThat(result.attributes()).containsExactly("Memory", "Screen");
    }

    @Test
    void getRelatedProductsBackoffice_returnsMappedProducts() {
        Product source = product(600L, "Source", "source", brand1, category1, 910L, 999.0, true);
        Product related = product(601L, "Related", "related", brand2, category2, 911L, 199.0, true);
        related.setParent(source);
        source.getRelatedProducts().add(ProductRelated.builder().product(source).relatedProduct(related).build());
        when(productRepository.findById(600L)).thenReturn(Optional.of(source));

        List<ProductListVm> result = productService.getRelatedProductsBackoffice(600L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Related");
        assertThat(result.getFirst().parentId()).isEqualTo(600L);
    }

    @Test
    void getRelatedProductsStorefront_returnsPublishedProductsOnly() {
        Product source = product(700L, "Source", "source-storefront", brand1, category1, 920L, 999.0, true);
        Product relatedPublished = product(701L, "Published", "published-related", brand1, category1, 921L, 199.0, true);
        Product relatedHidden = product(702L, "Hidden", "hidden-related", brand1, category1, 922L, 199.0, false);
        ProductRelated publishedRelation = ProductRelated.builder().product(source).relatedProduct(relatedPublished).build();
        ProductRelated hiddenRelation = ProductRelated.builder().product(source).relatedProduct(relatedHidden).build();
        when(productRepository.findById(700L)).thenReturn(Optional.of(source));
        when(productRelatedRepository.findAllByProduct(source, PageRequest.of(0, 2)))
            .thenReturn(new PageImpl<>(List.of(publishedRelation, hiddenRelation), PageRequest.of(0, 2), 2));

        ProductsGetVm result = productService.getRelatedProductsStorefront(700L, 0, 2);

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().getFirst().name()).isEqualTo("Published");
    }

    @Test
    void getProductsForWarehouse_mapsProducts() {
        when(productRepository.findProductForWarehouse("phone", "sku", List.of(1L, 2L), FilterExistInWhSelection.ALL.name()))
            .thenReturn(List.of(catalogProducts.get(0), catalogProducts.get(1)));

        List<ProductInfoVm> result = productService.getProductsForWarehouse(
            "phone", "sku", List.of(1L, 2L), FilterExistInWhSelection.ALL
        );

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().name()).isEqualTo("Product 1");
    }

    @Test
    void updateProductQuantity_updatesStockQuantity() {
        Product product1 = product(800L, "Product 800", "product-800", brand1, category1, 930L, 100.0, true);
        Product product2 = product(801L, "Product 801", "product-801", brand1, category1, 931L, 200.0, true);
        product1.setStockQuantity(10L);
        product2.setStockQuantity(20L);
        when(productRepository.findAllByIdIn(List.of(800L, 801L))).thenReturn(List.of(product1, product2));
        when(productRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        productService.updateProductQuantity(List.of(
            new com.yas.product.viewmodel.product.ProductQuantityPostVm(800L, 3L),
            new com.yas.product.viewmodel.product.ProductQuantityPostVm(801L, 5L)
        ));

        assertThat(product1.getStockQuantity()).isEqualTo(3L);
        assertThat(product2.getStockQuantity()).isEqualTo(5L);
    }

    @Test
    void subtractStockQuantity_mergesDuplicateItemsAndClampsToZero() {
        Product product1 = product(900L, "Product 900", "product-900", brand1, category1, 940L, 100.0, true);
        Product product2 = product(901L, "Product 901", "product-901", brand1, category1, 941L, 200.0, true);
        product1.setStockQuantity(10L);
        product2.setStockQuantity(4L);
        when(productRepository.findAllByIdIn(anyList())).thenAnswer(invocation -> {
            List<Long> productIds = invocation.getArgument(0);
            List<Product> result = new ArrayList<>();
            if (productIds.contains(900L)) {
                result.add(product1);
            }
            if (productIds.contains(901L)) {
                result.add(product2);
            }
            return result;
        });
        when(productRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        productService.subtractStockQuantity(List.of(
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(900L, 3L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(900L, 4L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(901L, 10L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(901L, 1L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(901L, 1L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(900L, 1L)
        ));

        assertThat(product1.getStockQuantity()).isEqualTo(2L);
        assertThat(product2.getStockQuantity()).isEqualTo(0L);
    }

    @Test
    void restoreStockQuantity_mergesDuplicateItems() {
        Product product1 = product(910L, "Product 910", "product-910", brand1, category1, 950L, 100.0, true);
        Product product2 = product(911L, "Product 911", "product-911", brand1, category1, 951L, 200.0, true);
        product1.setStockQuantity(10L);
        product2.setStockQuantity(4L);
        when(productRepository.findAllByIdIn(anyList())).thenAnswer(invocation -> {
            List<Long> productIds = invocation.getArgument(0);
            List<Product> result = new ArrayList<>();
            if (productIds.contains(910L)) {
                result.add(product1);
            }
            if (productIds.contains(911L)) {
                result.add(product2);
            }
            return result;
        });
        when(productRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        productService.restoreStockQuantity(List.of(
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(910L, 3L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(910L, 4L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(911L, 10L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(911L, 1L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(911L, 1L),
            new com.yas.product.viewmodel.product.ProductQuantityPutVm(910L, 1L)
        ));

        assertThat(product1.getStockQuantity()).isEqualTo(18L);
        assertThat(product2.getStockQuantity()).isEqualTo(16L);
    }

    @Test
    void getProductByIdsCategoryIdsAndBrandIds_returnMappedProducts() {
        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(catalogProducts.subList(0, 2));
        when(productRepository.findByCategoryIdsIn(List.of(20L))).thenReturn(catalogProducts.subList(0, 1));
        when(productRepository.findByBrandIdsIn(List.of(1L))).thenReturn(catalogProducts.subList(0, 2));

        List<ProductListVm> byIds = productService.getProductByIds(List.of(1L, 2L));
        List<ProductListVm> byCategoryIds = productService.getProductByCategoryIds(List.of(20L));
        List<ProductListVm> byBrandIds = productService.getProductByBrandIds(List.of(1L));

        assertThat(byIds).hasSize(2);
        assertThat(byCategoryIds).hasSize(1);
        assertThat(byBrandIds).hasSize(2);
    }

    @Test
    void getProductCheckoutList_returnsCheckoutViewModels() {
        Page<Product> productPage = new PageImpl<>(List.of(catalogProducts.get(0)), PageRequest.of(0, 2), 1);
        when(productRepository.findAllPublishedProductsByIds(List.of(1L), PageRequest.of(0, 2))).thenReturn(productPage);

        ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 2, List.of(1L));

        assertThat(result.productCheckoutListVms()).hasSize(1);
        ProductCheckoutListVm checkoutListVm = result.productCheckoutListVms().getFirst();
        assertThat(checkoutListVm.thumbnailUrl()).isEqualTo("https://media/101");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void exportProducts_returnsExportViewModels() {
        when(productRepository.getExportingProducts("product", "Brand One")).thenReturn(catalogProducts.subList(0, 2));

        List<ProductExportingDetailVm> result = productService.exportProducts(" Product ", "Brand One ");

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().brandName()).isEqualTo("Brand One");
    }

    private void initFixtures() {
        brand1 = new Brand();
        brand1.setId(1L);
        brand1.setName("Brand One");
        brand1.setSlug("brand-one");
        brand1.setPublished(true);

        brand2 = new Brand();
        brand2.setId(2L);
        brand2.setName("Brand Two");
        brand2.setSlug("brand-two");
        brand2.setPublished(true);

        category1 = new Category();
        category1.setId(20L);
        category1.setName("Category One");
        category1.setSlug("category-one");

        category2 = new Category();
        category2.setId(21L);
        category2.setName("Category Two");
        category2.setSlug("category-two");

        catalogProducts = IntStream.rangeClosed(1, 6)
            .mapToObj(this::catalogProduct)
            .toList();
    }

    private Product catalogProduct(int index) {
        Brand brand = index % 2 == 0 ? brand2 : brand1;
        Category category = index % 2 == 0 ? category2 : category1;
        Product product = product(
            (long) index,
            "Product " + index,
            "product-" + index,
            brand,
            category,
            100L + index,
            9.99 * index,
            true
        );
        product.getProductImages().add(ProductImage.builder().imageId(200L + index).product(product).build());
        return product;
    }

    private Product detailProduct() {
        Product product = product(500L, "Detail Product", "detail-product", brand1, category1, 301L, 49.99, true);
        product.getProductImages().add(ProductImage.builder().imageId(302L).product(product).build());
        product.getProductImages().add(ProductImage.builder().imageId(303L).product(product).build());

        ProductAttributeGroup group = new ProductAttributeGroup();
        group.setId(1000L);
        group.setName("Specs");

        ProductAttribute memoryAttribute = ProductAttribute.builder()
            .id(1001L)
            .name("Memory")
            .productAttributeGroup(group)
            .build();
        ProductAttribute screenAttribute = ProductAttribute.builder()
            .id(1002L)
            .name("Screen")
            .build();

        ProductAttributeValue groupedValue = new ProductAttributeValue();
        groupedValue.setId(1003L);
        groupedValue.setProduct(product);
        groupedValue.setProductAttribute(memoryAttribute);
        groupedValue.setValue("8 GB");

        ProductAttributeValue ungroupedValue = new ProductAttributeValue();
        ungroupedValue.setId(1004L);
        ungroupedValue.setProduct(product);
        ungroupedValue.setProductAttribute(screenAttribute);
        ungroupedValue.setValue("6.5 inch");

        product.getAttributeValues().add(groupedValue);
        product.getAttributeValues().add(ungroupedValue);
        return product;
    }

    private Product product(long id, String name, String slug, Brand brand, Category category,
                            Long thumbnailMediaId, double price, boolean published) {
        Product product = Product.builder()
            .id(id)
            .name(name)
            .slug(slug)
            .sku("SKU-" + id)
            .gtin("GTIN-" + id)
            .shortDescription("Short " + name)
            .description("Description " + name)
            .specification("Specification " + name)
            .price(price)
            .thumbnailMediaId(thumbnailMediaId)
            .isAllowedToOrder(true)
            .isPublished(published)
            .isFeatured(true)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .taxClassId(1L)
            .build();
        product.setBrand(brand);
        product.setCreatedOn(ZonedDateTime.now().minusDays(id));
        product.getProductCategories().add(ProductCategory.builder().product(product).category(category).build());
        return product;
    }

    private ProductOption productOption(Long id, String name) {
        ProductOption productOption = new ProductOption();
        productOption.setId(id);
        productOption.setName(name);
        return productOption;
    }

    private NoFileMediaVm media(Long id) {
        if (id == null) {
            return emptyMedia();
        }
        return new NoFileMediaVm(id, "caption-" + id, "file-" + id, "image/png", "https://media/" + id);
    }

    private NoFileMediaVm emptyMedia() {
        return new NoFileMediaVm(null, "", "", "", "");
    }
}