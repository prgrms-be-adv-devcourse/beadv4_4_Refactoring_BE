package com.thock.back.product.in;

import com.thock.back.global.security.AuthUser;
import com.thock.back.global.security.AuthenticatedUser;
import com.thock.back.product.app.ProductCreateService;
import com.thock.back.product.app.ProductManageService;
import com.thock.back.product.app.ProductQueryService;
import com.thock.back.product.domain.Category;
import com.thock.back.product.in.dto.ProductCreateRequest;
import com.thock.back.product.in.dto.ProductDetailResponse;
import com.thock.back.product.in.dto.ProductListResponse;
import com.thock.back.product.in.dto.ProductUpdateRequest;
import com.thock.back.product.in.dto.internal.ProductInternalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "product-controller", description = "상품 관련 API (등록, 조회, 수정, 삭제)")
public class ProductController {

    private final ProductCreateService productCreateService;
    private final ProductQueryService productQueryService;
    private final ProductManageService productManageService;

    @Operation(summary = "상품 등록", description = "판매자가 새로운 상품을 등록합니다. (판매자 권한 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (판매자만 등록 가능)")
    })
    @PostMapping
    public ResponseEntity<Long> createProduct(
            @RequestBody @Valid ProductCreateRequest request,
            @AuthUser AuthenticatedUser user
    ) {
        Long productId = productCreateService.createProduct(
                request.toCommand(user.memberId(), user.role())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    @Operation(summary = "카테고리별 상품 리스트 조회", description = "카테고리별로 상품 리스트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<Page<ProductListResponse>> getProductList(
            @RequestParam Category category,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productQueryService.searchProductsByCategory(category, pageable));
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID를 통해 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품입니다.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProduct(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(productQueryService.getProductById(id));
    }

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다. (본인 상품 또는 관리자만 수정 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 상품만 수정 가능)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품입니다.")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Long> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request,
            @AuthUser AuthenticatedUser user
    ) {
        Long productId = productManageService.updateProduct(
                request.toCommand(id, user.memberId(), user.role())
        );
        return ResponseEntity.ok(productId);
    }

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다. (본인 혹은 관리자만 삭제 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품입니다.")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthUser AuthenticatedUser user
    ) {
        productManageService.deleteProduct(id, user.memberId(), user.role());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 검색", description = "키워드로 상품을 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ProductListResponse>> searchProducts(
            @Parameter(description = "검색어") @RequestParam String keyword
    ) {
        return ResponseEntity.ok(productQueryService.searchProductsByKeyword(keyword));
    }

    @Operation(
            summary = "[내부용] 상품 ID 리스트로 정보 조회",
            description = "마켓(장바구니), 정산 모듈 등에서 상품 ID 리스트를 받아 핵심 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/internal/list")
    public ResponseEntity<List<ProductInternalResponse>> getProductsByIds(
            @RequestBody @NotEmpty List<@NotNull Long> productIds
    ) {
        return ResponseEntity.ok(productQueryService.getProductsByIds(productIds));
    }

    @Operation(
            summary = "내가 등록한 상품 리스트 조회",
            description = "판매자가 본인이 등록한 상품들을 페이지네이션으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<ProductListResponse>> getMyProducts(
            @AuthUser AuthenticatedUser user,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productQueryService.getMyProducts(user.memberId(), pageable));
    }
}