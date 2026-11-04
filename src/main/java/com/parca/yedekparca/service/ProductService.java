package com.parca.yedekparca.service;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;

    public ProductService(
            ProductRepository productRepository,
            UserRepository userRepository,
            SellerRepository sellerRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public ProductResponse createSellerProduct(String email, ProductCreateRequest request) {
        Seller seller = getApprovedSeller(email);
        ProductInput input = ProductInput.from(request);
        validateInput(input);

        Product product = Product.builder()
                .seller(seller)
                .moderationStatus(ProductModerationStatus.PENDING)
                .active(true)
                .build();
        applyInput(product, input);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductPageResponse<ProductResponse> getSellerProducts(
            String email, String search, ProductModerationStatus status, int page, int size) {
        Seller seller = getApprovedSeller(email);
        ProductSearchCriteria criteria = emptyCriteria(search, status, seller.getId());
        Page<Product> products = productRepository.findAll(
                ProductSpecifications.forSeller(criteria), pageRequest(page, size, "createdAt", "desc"));
        return mapPage(products, this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getSellerProduct(String email, Long productId) {
        Seller seller = getApprovedSeller(email);
        Product product = productRepository.findByIdAndSellerId(productId, seller.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateSellerProduct(String email, Long productId, ProductUpdateRequest request) {
        Seller seller = getApprovedSeller(email);
        Product product = productRepository.findByIdAndSellerId(productId, seller.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        ProductInput input = ProductInput.from(request);
        validateInput(input);

        boolean moderationFieldsChanged = moderationFieldsChanged(product, input);
        applyInput(product, input);
        if (moderationFieldsChanged && product.getModerationStatus() != ProductModerationStatus.PENDING) {
            product.setModerationStatus(ProductModerationStatus.PENDING);
            product.setRejectionReason(null);
            product.setReviewedAt(null);
        }

        return toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductPageResponse<ProductResponse> getAdminProducts(
            ProductSearchCriteria criteria, int page, int size, String sortBy, String direction) {
        Page<Product> products = productRepository.findAll(
                ProductSpecifications.forAdmin(criteria), pageRequest(page, size, sortBy, direction));
        return mapPage(products, this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getAdminProduct(Long productId) {
        return productRepository.findById(productId)
                .map(this::toProductResponse)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
    }

    @Transactional
    public ProductResponse approveProduct(Long productId) {
        Product product = getPendingProduct(productId);
        product.setModerationStatus(ProductModerationStatus.APPROVED);
        product.setRejectionReason(null);
        product.setReviewedAt(LocalDateTime.now());
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse rejectProduct(Long productId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Ret gerekçesi zorunludur.");
        }
        Product product = getPendingProduct(productId);
        product.setModerationStatus(ProductModerationStatus.REJECTED);
        product.setRejectionReason(rejectionReason.trim());
        product.setReviewedAt(LocalDateTime.now());
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateActiveStatus(Long productId, boolean active) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        product.setActive(active);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductPageResponse<PublicProductResponse> getPublicProducts(
            ProductSearchCriteria criteria, int page, int size, String sortBy, String direction) {
        validatePriceRange(criteria.minPrice(), criteria.maxPrice());
        Page<Product> products = productRepository.findAll(
                ProductSpecifications.forCatalog(criteria), pageRequest(page, size, sortBy, direction));
        return mapPage(products, this::toPublicProductResponse);
    }

    @Transactional(readOnly = true)
    public PublicProductResponse getPublicProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(item -> item.getModerationStatus() == ProductModerationStatus.APPROVED
                        && Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        return toPublicProductResponse(product);
    }

    private Seller getApprovedSeller(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        if (user.getRole() != UserRole.SELLER) {
            throw new IllegalArgumentException("Onaylı satıcı hesabı gereklidir.");
        }
        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Onaylı satıcı hesabı gereklidir."));
        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new IllegalArgumentException("Satıcı başvurunuz onaylanmamış.");
        }
        return seller;
    }

    private Product getPendingProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        if (product.getModerationStatus() != ProductModerationStatus.PENDING) {
            throw new IllegalArgumentException("Yalnızca bekleyen ürünler değerlendirilebilir.");
        }
        return product;
    }

    private void validateInput(ProductInput input) {
        if (input.price() == null || input.condition() == null) {
            throw new IllegalArgumentException("Ürün fiyatı ve durumu zorunludur.");
        }
        validatePriceRange(input.price(), null);
        if (input.price().scale() > 2 || input.price().precision() > 12) {
            throw new IllegalArgumentException("Fiyat en fazla iki ondalık basamak içerebilir.");
        }
        if (input.stockQuantity() == null || input.stockQuantity() < 0) {
            throw new IllegalArgumentException("Stok miktarı negatif olamaz.");
        }
        validateImageUrl(input.imageUrl());
        Set<CompatibilityKey> keys = new HashSet<>();
        for (ProductVehicleCompatibilityRequest compatibility : input.vehicleCompatibilities()) {
            if (compatibility.getYearFrom() == null || compatibility.getYearTo() == null
                    || compatibility.getYearFrom() > compatibility.getYearTo()) {
                throw new IllegalArgumentException("Araç yıl aralığı geçersiz.");
            }
            CompatibilityKey key = CompatibilityKey.from(compatibility);
            if (!keys.add(key)) {
                throw new IllegalArgumentException("Aynı araç uyumluluğu birden fazla eklenemez.");
            }
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            URI uri = new URI(imageUrl.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Görsel adresi HTTP veya HTTPS olmalıdır.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Görsel adresi geçersiz.");
        }
    }

    private boolean moderationFieldsChanged(Product product, ProductInput input) {
        return !Objects.equals(product.getName(), clean(input.name()))
                || !Objects.equals(product.getDescription(), cleanNullable(input.description()))
                || !Objects.equals(product.getBrand(), clean(input.brand()))
                || !Objects.equals(product.getOemNumber(), cleanNullable(input.oemNumber()))
                || !Objects.equals(product.getCategory(), clean(input.category()))
                || !compatibilityKeys(product.getVehicleCompatibilities()).equals(
                        compatibilityKeysFromRequests(input.vehicleCompatibilities()));
    }

    private Set<CompatibilityKey> compatibilityKeys(List<ProductVehicleCompatibility> items) {
        Set<CompatibilityKey> keys = new HashSet<>();
        for (ProductVehicleCompatibility item : items) {
            keys.add(new CompatibilityKey(normalize(item.getVehicleBrand()), normalize(item.getVehicleModel()),
                    item.getYearFrom(), item.getYearTo(), normalize(item.getEngineInfo())));
        }
        return keys;
    }

    private Set<CompatibilityKey> compatibilityKeysFromRequests(List<ProductVehicleCompatibilityRequest> items) {
        Set<CompatibilityKey> keys = new HashSet<>();
        for (ProductVehicleCompatibilityRequest item : items) keys.add(CompatibilityKey.from(item));
        return keys;
    }

    private void applyInput(Product product, ProductInput input) {
        product.setName(clean(input.name()));
        product.setDescription(cleanNullable(input.description()));
        product.setCategory(clean(input.category()));
        product.setBrand(clean(input.brand()));
        product.setOemNumber(cleanNullable(input.oemNumber()));
        product.setPrice(input.price());
        product.setStockQuantity(input.stockQuantity());
        product.setCondition(input.condition());
        product.setImageUrl(cleanNullable(input.imageUrl()));

        product.getVehicleCompatibilities().clear();
        for (ProductVehicleCompatibilityRequest request : input.vehicleCompatibilities()) {
            product.getVehicleCompatibilities().add(ProductVehicleCompatibility.builder()
                    .product(product)
                    .vehicleBrand(clean(request.getVehicleBrand()))
                    .vehicleModel(clean(request.getVehicleModel()))
                    .yearFrom(request.getYearFrom())
                    .yearTo(request.getYearTo())
                    .engineInfo(cleanNullable(request.getEngineInfo()))
                    .build());
        }
    }

    private ProductResponse toProductResponse(Product product) {
        User sellerUser = product.getSeller().getUser();
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerStoreName(product.getSeller().getStoreName())
                .sellerFirstName(sellerUser.getFirstName())
                .sellerLastName(sellerUser.getLastName())
                .sellerEmail(sellerUser.getEmail())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .oemNumber(product.getOemNumber())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .condition(product.getCondition())
                .imageUrl(product.getImageUrl())
                .vehicleCompatibilities(mapCompatibilities(product))
                .moderationStatus(product.getModerationStatus())
                .rejectionReason(product.getRejectionReason())
                .reviewedAt(product.getReviewedAt())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private PublicProductResponse toPublicProductResponse(Product product) {
        return PublicProductResponse.builder()
                .id(product.getId())
                .sellerStoreName(product.getSeller().getStoreName())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .oemNumber(product.getOemNumber())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .condition(product.getCondition())
                .imageUrl(product.getImageUrl())
                .vehicleCompatibilities(mapCompatibilities(product))
                .createdAt(product.getCreatedAt())
                .build();
    }

    private List<ProductVehicleCompatibilityResponse> mapCompatibilities(Product product) {
        return product.getVehicleCompatibilities().stream().map(item -> ProductVehicleCompatibilityResponse.builder()
                .id(item.getId())
                .vehicleBrand(item.getVehicleBrand())
                .vehicleModel(item.getVehicleModel())
                .yearFrom(item.getYearFrom())
                .yearTo(item.getYearTo())
                .engineInfo(item.getEngineInfo())
                .build()).toList();
    }

    private <T, R> ProductPageResponse<R> mapPage(Page<T> page, java.util.function.Function<T, R> mapper) {
        return ProductPageResponse.<R>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private PageRequest pageRequest(int page, int size, String sortBy, String direction) {
        Set<String> allowedSorts = Set.of("createdAt", "price", "name");
        if (!allowedSorts.contains(sortBy)) {
            throw new IllegalArgumentException("Sıralama alanı geçersiz.");
        }
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    private ProductSearchCriteria emptyCriteria(
            String search, ProductModerationStatus status, Long sellerId) {
        return new ProductSearchCriteria(search, null, null, null, null, null,
                null, null, null, null, null, sellerId, status, null);
    }

    private void validatePriceRange(BigDecimal min, BigDecimal max) {
        if (min != null && min.signum() < 0 || max != null && max.signum() < 0) {
            throw new IllegalArgumentException("Fiyat negatif olamaz.");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimum fiyat maksimum fiyattan büyük olamaz.");
        }
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
    private String cleanNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    private record ProductInput(
            String name, String description, String category, String brand, String oemNumber,
            BigDecimal price, Integer stockQuantity, ProductCondition condition, String imageUrl,
            List<ProductVehicleCompatibilityRequest> vehicleCompatibilities) {
        static ProductInput from(ProductCreateRequest request) {
            return new ProductInput(request.getName(), request.getDescription(), request.getCategory(),
                    request.getBrand(), request.getOemNumber(), request.getPrice(), request.getStockQuantity(),
                    request.getCondition(), request.getImageUrl(),
                    request.getVehicleCompatibilities() == null ? List.of() : request.getVehicleCompatibilities());
        }
        static ProductInput from(ProductUpdateRequest request) {
            return new ProductInput(request.getName(), request.getDescription(), request.getCategory(),
                    request.getBrand(), request.getOemNumber(), request.getPrice(), request.getStockQuantity(),
                    request.getCondition(), request.getImageUrl(),
                    request.getVehicleCompatibilities() == null ? List.of() : request.getVehicleCompatibilities());
        }
    }

    private record CompatibilityKey(String brand, String model, Integer yearFrom, Integer yearTo, String engine) {
        static CompatibilityKey from(ProductVehicleCompatibilityRequest request) {
            return new CompatibilityKey(request.getVehicleBrand().trim().toLowerCase(Locale.ROOT),
                    request.getVehicleModel().trim().toLowerCase(Locale.ROOT), request.getYearFrom(),
                    request.getYearTo(), request.getEngineInfo() == null ? ""
                    : request.getEngineInfo().trim().toLowerCase(Locale.ROOT));
        }
    }
}
