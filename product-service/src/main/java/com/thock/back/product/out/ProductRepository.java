package com.thock.back.product.out;

import com.thock.back.product.domain.Category;
import com.thock.back.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Pageable 객체를 다루는 기능. 리스트 반환 대신 Page로 반환
    Page<Product> findByCategory(Category category, Pageable pageable);

    // 이름에 keyword가 포함된 것 찾기 (ex: 브랜드 이름, 키보드 등등)
    // select * from products where name like %keyword% 와 같음
    List<Product> findByNameContaining(String keyword);

    // 이름이나 설명에 keword가 포함된 것 찾기 (ex: 무접점, 적축, 갈축 등등)
    List<Product> findByNameContainingOrDescriptionContaining(String name, String keyword);

    List<Product> findAllByIdIn(List<Long> ids);

    // 판매자가 등록한 자신의 상품 조회 - 판매자 페이지에서 이용
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
}
