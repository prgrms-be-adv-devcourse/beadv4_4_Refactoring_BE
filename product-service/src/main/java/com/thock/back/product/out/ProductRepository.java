package com.thock.back.product.out;

import com.thock.back.product.domain.Category;
import com.thock.back.product.domain.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상품명으로 검색 (부분 일치)
    List<Product> findByNameContaining(String keyword);

    // 카테고리로 검색 (페이징)
    Page<Product> findByCategory(Category category, Pageable pageable);

    // 여러 상품 ID로 조회
    List<Product> findAllByIdIn(List<Long> ids);

    // 여러 상품 ID로 조회하면서 PESSIMISTIC_WRITE 락을 거는 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    // 판매자 ID로 상품 조회 (페이징)
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
}
