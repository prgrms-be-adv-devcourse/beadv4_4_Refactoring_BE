package com.thock.back.api.boundedContext.product.out;

import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);

    // 이름에 keyword가 포함된 것 찾기 (ex: 브랜드 이름, 키보드 등등)
    // select * from products where name like %keyword% 와 같음
    List<Product> findByNameContaining(String keyword);

    // 이름이나 설명에 keword가 포함된 것 찾기 (ex: 무접점, 적축, 갈축 등등)
    List<Product> findByNameContainingOrDescriptionContaining(String name, String keyword);
}
