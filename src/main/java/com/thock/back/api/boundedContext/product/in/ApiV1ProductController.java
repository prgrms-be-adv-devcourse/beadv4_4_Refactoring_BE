package com.thock.back.api.boundedContext.product.in;

import com.thock.back.api.boundedContext.product.app.ProductService;
import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.in.dto.ProductCreateRequest;
import com.thock.back.api.boundedContext.product.in.dto.ProductDetailResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductListResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductUpdateRequest;
import com.thock.back.api.boundedContext.product.in.dto.internal.ProductInternalResponse;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.dto.MemberDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
//TODO JWT 완료 되면 수정하기
public class ApiV1ProductController {
    private final ProductService productService;


    // 1. 상품 등록
    @Operation(summary = "상품 등록", description = "판매자가 새로운 상품을 등록합니다. (판매자 권한 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락, 가격 0원 이하 등)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (판매자만 등록 가능)")
    })
    // 요청: POST /api/v1/products?category=KEYBOARD
    @PostMapping
    public ResponseEntity<Long> create(
            @RequestBody @Valid ProductCreateRequest request,
            // [임시] JWT 구현 전까지 이걸로 테스트. 나중에 이 두 줄만 지우면 됨.
            @Parameter(hidden = true)@RequestHeader(value = "X-Member-Id", defaultValue = "1") Long memberId,
            @Parameter(hidden = true)@RequestHeader(value = "X-Member-Role", defaultValue = "SELLER") String roleStr
    ) {
        // 임시 멤버 객체 생성
        MemberDto member = MemberDto.builder()
                .id(memberId)
                .role(MemberRole.valueOf(roleStr))
                .build();

        Long productId = productService.productCreate(request, member);

        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 2. 카테고리별 상품 조회
    @Operation(summary = "카테고리별 상품 리스트 조회", description = "카테고리별로 상품을 리스트를 보여줍니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    // 요청: GET /api/v1/products?category=KEYBOARD
    @GetMapping
    public ResponseEntity<Page<ProductListResponse>> list(
            @RequestParam Category category,
            // 프론트가 ?page=1&size=10 처럼 보내면 알아서 Pageable 객체로 만들어줌
            // 기본값: 0페이지(첫페이지), 10개씩, 최신순(id 내림차순)
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(productService.searchByCategory(category, pageable));
    }


    // 3. 상품 상세조회(R)
    @Operation(summary = "상품 상세 조회", description = "상품 ID를 통해 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품입니다.")
    })
    // 요청: GET /api/v1/products/1
    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.productDetail(id);
    }


    // 4. 상품 수정(U)
    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다. (본인 상품만 수정 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 상품만 수정 가능)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품입니다.")
    })
    // 요청: PUT /api/v1/products/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Long> update(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-Member-Id", defaultValue = "1") Long memberId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Member-Role", defaultValue = "SELLER") String roleStr
    ) {
        MemberDto member = MemberDto.builder()
                .id(memberId)
                .role(MemberRole.valueOf(roleStr))
                .build();

        Long productId = productService.productUpdate(id, request, member);

        return ResponseEntity.ok(productId);
    }

    // 5. 상품 삭제(D)
    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다. (본인 혹은 관리자만 삭제 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공 (반환값 없음)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 혹은 관리자만 삭제 가능)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품입니다.")
    })
    // DELETE /api/v1/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "X-Member-Id", defaultValue = "1") Long memberId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Member-Role", defaultValue = "SELLER") String roleStr
    ) {
        MemberDto member = MemberDto.builder()
                .id(memberId)
                .role(MemberRole.valueOf(roleStr))
                .build();

        productService.productDelete(id, member);
        return ResponseEntity.noContent().build();
    }

    // 6. 상품 검색
    @Operation(summary = "상품 검색", description = "키워드로 상품을 검색합니다.")
    @GetMapping("/search")
    // GET /api/v1/products/search
    public ResponseEntity<List<ProductListResponse>> search(
            @Parameter(description = "검색어") @RequestParam String keyword
    ) {
        return ResponseEntity.ok(productService.searchByKeyword(keyword));
    }

    // 7. Market 내부 통신 ID 리스트로 상품 정보 조회
    @Operation(
            summary = "[내부용] 상품 ID 리스트로 정보 조회",
            description = "마켓(장바구니), 정산 모듈 등에서 <b>상품 ID 리스트</b>를 받아 핵심 정보를 조회합니다.<br>" +
                    "URL 길이 제한 이슈를 피하기 위해 <b>POST</b> 방식을 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (존재하는 상품만 리스트로 반환)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 리스트 등)")
    })
    @PostMapping("/internal/list")
    public ResponseEntity<List<ProductInternalResponse>> getProductsByIds(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "조회할 상품 ID 리스트",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            // ▼ 이게 있으면 Swagger에서 클릭 한 번으로 [1, 2, 3] 입력됨!
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "[1, 2, 3]")
                    )
            )
            @RequestBody List<Long> productIds
    ) {
        List<ProductInternalResponse> responses = productService.getProductsByIds(productIds);
        return ResponseEntity.ok(responses);
    }
}