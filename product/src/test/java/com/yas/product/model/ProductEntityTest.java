package com.yas.product.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.product.model.attribute.ProductAttributeGroup;
import org.junit.jupiter.api.Test;

class ProductEntityTest {

    @Test
    void product_equalsAndHashCode_useIdentifier() {
        Product first = new Product();
        first.setId(1L);
        Product second = new Product();
        second.setId(1L);
        Product different = new Product();
        different.setId(2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void brand_equalsAndHashCode_useIdentifier() {
        Brand first = new Brand();
        first.setId(1L);
        Brand second = new Brand();
        second.setId(1L);
        Brand different = new Brand();
        different.setId(2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void category_equalsAndHashCode_useIdentifier() {
        Category first = new Category();
        first.setId(1L);
        Category second = new Category();
        second.setId(1L);
        Category different = new Category();
        different.setId(2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void productOption_equalsAndHashCode_useIdentifier() {
        ProductOption first = new ProductOption();
        first.setId(1L);
        ProductOption second = new ProductOption();
        second.setId(1L);
        ProductOption different = new ProductOption();
        different.setId(2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void productOptionCombination_equalsAndHashCode_useIdentifier() {
        ProductOptionCombination first = new ProductOptionCombination();
        first.setId(1L);
        ProductOptionCombination second = new ProductOptionCombination();
        second.setId(1L);
        ProductOptionCombination different = new ProductOptionCombination();
        different.setId(2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void productRelated_equalsAndHashCode_useIdentifier() {
        ProductRelated first = ProductRelated.builder().id(1L).build();
        ProductRelated second = ProductRelated.builder().id(1L).build();
        ProductRelated different = ProductRelated.builder().id(2L).build();

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void productOptionValue_equalsAndHashCode_useIdentifier() {
        ProductOptionValue first = ProductOptionValue.builder().id(1L).build();
        ProductOptionValue second = ProductOptionValue.builder().id(1L).build();
        ProductOptionValue different = ProductOptionValue.builder().id(2L).build();

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void productAttributeGroup_equalsAndHashCode_useIdentifier() {
        ProductAttributeGroup first = new ProductAttributeGroup();
        first.setId(1L);
        ProductAttributeGroup second = new ProductAttributeGroup();
        second.setId(1L);
        ProductAttributeGroup different = new ProductAttributeGroup();
        different.setId(2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }
}