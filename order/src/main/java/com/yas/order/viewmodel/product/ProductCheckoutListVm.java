package com.yas.order.viewmodel.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCheckoutListVm {
    private Long id;
    private String name;
    private Double price;
    private Long taxClassId;

    public ProductCheckoutListVm() {
    }

    public ProductCheckoutListVm(Long id, String name, Double price, Long taxClassId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.taxClassId = taxClassId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getTaxClassId() {
        return taxClassId;
    }

    public void setTaxClassId(Long taxClassId) {
        this.taxClassId = taxClassId;
    }
}


