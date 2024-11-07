package com.med.medStore.Services;

import org.springframework.data.jpa.repository.JpaRepository;

import com.med.medStore.models.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

}
