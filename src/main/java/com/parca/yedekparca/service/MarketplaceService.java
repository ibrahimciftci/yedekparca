package com.parca.yedekparca.service;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.*;
import java.time.*;
import java.util.*;

@Service
public class MarketplaceService {
    private final CatalogProductRepository catalogs;
    private final ProductOfferRepository offers;
    private final SellerRepository sellers;
    private final UserRepository users;
    private final OfferPhotoRepository photos;
    private final ObjectStorageService storage;
    private final UserFavoriteRepository favorites;
    private final ComplaintRepository complaints;
    private final ProductRepository legacyProducts;

    public MarketplaceService(CatalogProductRepository c, ProductOfferRepository o, SellerRepository s, UserRepository u, OfferPhotoRepository p, ObjectStorageService st, UserFavoriteRepository f, ComplaintRepository cr, ProductRepository legacyProducts) {
        catalogs = c;
        offers = o;
        sellers = s;
        users = u;
        photos = p;
        storage = st;
        favorites = f;
        complaints = cr;
        this.legacyProducts = legacyProducts;
    }

    private Seller approvedSeller(String email) {
        User u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        if (u.getRole() != UserRole.SELLER) throw new IllegalArgumentException("Satıcı rolü gereklidir.");
        Seller s = sellers.findByUserId(u.getId()).orElseThrow(() -> new IllegalArgumentException("Satıcı profili bulunamadı."));
        if (s.getStatus() != SellerStatus.APPROVED || !Boolean.TRUE.equals(u.getActive()))
            throw new IllegalArgumentException("Onaylı ve aktif satıcı gereklidir.");
        return s;
    }

    private User currentUser(String email) {
        User u = users.findByEmail(email).filter(x -> Boolean.TRUE.equals(x.getActive())).orElseThrow(() -> new IllegalArgumentException("Aktif kullanıcı bulunamadı."));
        if (u.getRole() != UserRole.CUSTOMER) throw new IllegalArgumentException("Bu işlem müşteri hesabı gerektirir.");
        return u;
    }

    @Transactional
    public SellerProfileView updateProfile(String email, SellerProfileUpdateRequest r) {
        Seller s = approvedSeller(email);
        s.setStoreName(r.storeName().trim());
        s.setPhone(r.phone().trim());
        s.setWhatsappPhone(blank(r.whatsappPhone()));
        s.setContactEmail(blank(r.contactEmail()));
        s.setCity(r.city().trim());
        s.setDistrict(r.district().trim());
        s.setStreetAddress(blank(r.streetAddress()));
        s.setLatitude(r.latitude());
        s.setLongitude(r.longitude());
        String hours = blank(r.workingHours());
        if (hours != null && !((hours.startsWith("{") && hours.endsWith("}")) || (hours.startsWith("[") && hours.endsWith("]"))))
            throw new IllegalArgumentException("Çalışma saatleri geçerli JSON olmalıdır.");
        s.setWorkingHours(hours);
        s.setDescription(blank(r.description()));
        s.setWebsiteUrl(blank(r.websiteUrl()));
        s.setLogoUrl(blank(r.logoUrl()));
        return profile(s);
    }

    @Transactional(readOnly = true)
    public SellerProfileView myProfile(String email) {
        return profile(approvedSeller(email));
    }

    @Transactional(readOnly = true)
    public SellerProfileView publicProfile(Long id) {
        Seller s = sellers.findById(id).filter(x -> x.getStatus() == SellerStatus.APPROVED && Boolean.TRUE.equals(x.getProfileActive()) && Boolean.TRUE.equals(x.getUser().getActive())).orElseThrow(() -> new IllegalArgumentException("Firma profili bulunamadı."));
        return profile(s);
    }

    @Transactional(readOnly = true)
    public SellerProfileView adminSellerProfile(Long id) {
        return profile(sellers.findById(id).orElseThrow(() -> new IllegalArgumentException("Firma profili bulunamadı.")));
    }

    @Transactional
    public SellerProfileView setSellerProfileActive(Long id, boolean active) {
        Seller seller = sellers.findById(id).orElseThrow(() -> new IllegalArgumentException("Seller profile not found."));
        seller.setProfileActive(active);
        return profile(seller);
    }

    private SellerProfileView profile(Seller s) {
        return new SellerProfileView(s.getId(), s.getStoreName(), s.getPhone(), s.getWhatsappPhone(), s.getContactEmail(), s.getCity(), s.getDistrict(), s.getStreetAddress(), s.getLatitude(), s.getLongitude(), s.getWorkingHours(), s.getDescription(), s.getWebsiteUrl(), s.getLogoUrl(), s.getProfileActive());
    }

    @Transactional
    public OfferView createOffer(String email, OfferWriteRequest r) {
        Seller s = approvedSeller(email);
        CatalogProduct c;
        if (r.catalogProductId() != null) {
            c = catalogs.findById(r.catalogProductId()).orElseThrow(() -> new IllegalArgumentException("Katalog ürünü bulunamadı."));
            if (c.getModerationStatus() != CatalogModerationStatus.APPROVED || !c.getActive())
                throw new IllegalArgumentException("Yalnızca onaylı katalog ürününe ilan verilebilir.");
        } else {
            c = catalogs.save(CatalogProduct.builder().name(r.name().trim()).description(blank(r.description())).category(r.category().trim()).brand(r.brand().trim()).oemNumber(blank(r.oemNumber())).moderationStatus(CatalogModerationStatus.PENDING).active(true).build());
            replaceCompatibility(c, r.vehicleCompatibilities());
        }
        if (r.price().signum() <= 0) throw new IllegalArgumentException("Fiyat sıfırdan büyük olmalıdır.");
        if (offers.existsBySellerIdAndCatalogProductIdAndCondition(s.getId(), c.getId(), r.condition())) {
            throw new IllegalArgumentException("Firm already has an offer for this product condition.");
        }
        ProductOffer o = ProductOffer.builder().seller(s).catalogProduct(c).price(r.price()).stockQuantity(r.stockQuantity()).condition(r.condition()).offerDescription(blank(r.offerDescription() == null ? r.description() : r.offerDescription())).moderationStatus(ProductModerationStatus.PENDING).active(true).build();
        return view(offers.save(o));
    }

    @Transactional(readOnly = true)
    public Page<OfferView> myOffers(String email, int page, int size) {
        Seller s = approvedSeller(email);
        return offers.findAll(MarketplaceSpecifications.offers(null, null, s.getId(), null, null, false), PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::view);
    }

    @Transactional(readOnly = true)
    public OfferView myOffer(String email, Long id) {
        Seller s = approvedSeller(email);
        return view(offers.findByIdAndSellerId(id, s.getId()).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı.")));
    }

    @Transactional
    public OfferView updateOffer(String email, Long id, OfferWriteRequest r) {
        Seller s = approvedSeller(email);
        ProductOffer o = offers.findByIdAndSellerId(id, s.getId()).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı."));
        if (r.price().signum() <= 0) throw new IllegalArgumentException("Fiyat sıfırdan büyük olmalıdır.");
        CatalogProduct targetCatalog = o.getCatalogProduct();
        boolean catalogChanged = false;
        if (r.catalogProductId() == null) {
            boolean detailsChanged = !Objects.equals(targetCatalog.getName(), r.name().trim())
                    || !Objects.equals(targetCatalog.getCategory(), r.category().trim())
                    || !Objects.equals(targetCatalog.getBrand(), r.brand().trim())
                    || !Objects.equals(targetCatalog.getOemNumber(), blank(r.oemNumber()))
                    || !Objects.equals(targetCatalog.getDescription(), blank(r.description()));
            if (detailsChanged) {
                targetCatalog = catalogs.save(CatalogProduct.builder().name(r.name().trim())
                        .description(blank(r.description())).category(r.category().trim()).brand(r.brand().trim())
                        .oemNumber(blank(r.oemNumber())).moderationStatus(CatalogModerationStatus.PENDING)
                        .active(true).build());
                replaceCompatibility(targetCatalog, r.vehicleCompatibilities());
                catalogChanged = true;
            }
        } else if (!Objects.equals(r.catalogProductId(), targetCatalog.getId())) {
            targetCatalog = catalogs.findById(r.catalogProductId())
                    .filter(c -> c.getModerationStatus() == CatalogModerationStatus.APPROVED && Boolean.TRUE.equals(c.getActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Only approved active catalog products can be selected."));
            catalogChanged = true;
        }
        if (catalogChanged && offers.existsBySellerIdAndCatalogProductIdAndCondition(s.getId(), targetCatalog.getId(), r.condition()))
            throw new IllegalArgumentException("An offer for this catalog product and condition already exists.");
        if (o.getPrice().compareTo(r.price()) != 0) {
            o.setPrice(r.price());
            o.setPriceUpdatedAt(LocalDateTime.now());
        }
        if (!Objects.equals(o.getStockQuantity(), r.stockQuantity())) {
            o.setStockQuantity(r.stockQuantity());
            o.setStockUpdatedAt(LocalDateTime.now());
        }
        if (!catalogChanged && o.getCondition() != r.condition()
                && offers.existsBySellerIdAndCatalogProductIdAndCondition(s.getId(), targetCatalog.getId(), r.condition())) {
            throw new IllegalArgumentException("Firm already has an offer for this product condition.");
        }
        String offerDescription = blank(r.offerDescription() == null ? r.description() : r.offerDescription());
        boolean reviewChanged = catalogChanged || o.getCondition() != r.condition() || !Objects.equals(o.getOfferDescription(), offerDescription);
        o.setCatalogProduct(targetCatalog);
        o.setCondition(r.condition());
        o.setOfferDescription(offerDescription);
        if (reviewChanged && o.getModerationStatus() != ProductModerationStatus.PENDING) {
            o.setModerationStatus(ProductModerationStatus.PENDING);
            o.setRejectionReason(null);
            o.setReviewedAt(null);
        }
        return view(o);
    }

    @Transactional(readOnly = true)
    public Page<OfferView> publicOffers(String search, ProductCondition condition, int page, int size) {
        return offers.findAll(MarketplaceSpecifications.offers(search, condition, null, null, null, true), PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "price"))).map(this::view);
    }

    @Transactional(readOnly = true)
    public CatalogView catalogDetail(Long id) {
        CatalogProduct c = catalogs.findById(id).filter(x -> x.getModerationStatus() == CatalogModerationStatus.APPROVED && x.getActive()).orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        List<OfferView> list = offers.findPublicByCatalog(id).stream().map(this::view).toList();
        return catalogView(c, list);
    }

    @Transactional(readOnly = true)
    public Page<CatalogView> publicCatalog(String search, String category, String brand, String oem, java.math.BigDecimal min, java.math.BigDecimal max, ProductCondition condition, String vehicleBrand, String vehicleModel, Integer year, String engine, int page, int size) {
        if (min != null && min.signum() < 0 || max != null && max.signum() < 0 || min != null && max != null && min.compareTo(max) > 0)
            throw new IllegalArgumentException("Fiyat filtresi geçersiz.");
        Page<CatalogProduct> p = catalogs.findAll(CatalogSpecifications.publicSearch(search, category, brand, oem, min, max, condition, vehicleBrand, vehicleModel, year, engine), PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return p.map(c -> {
            List<OfferView> valid = offers.findPublicByCatalog(c.getId()).stream().filter(o -> condition == null || o.getCondition() == condition).filter(o -> min == null || o.getPrice().compareTo(min) >= 0).filter(o -> max == null || o.getPrice().compareTo(max) <= 0).map(this::view).toList();
            return catalogView(c, valid);
        });
    }

    @Transactional(readOnly = true)
    public Page<OfferView> adminOffers(ProductModerationStatus status, int page, int size) {
        return adminOffers(null, null, null, null, status, null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<OfferView> adminOffers(String search, String category, String brand, Long sellerId,
                                       ProductModerationStatus status, Boolean active, int page, int size) {
        return offers.findAll(MarketplaceSpecifications.offers(search, category, brand, null, sellerId, status, active, false),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::view);
    }

    @Transactional(readOnly = true)
    public OfferView adminOffer(Long id) {
        return view(offers.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found.")));
    }

    @Transactional(readOnly = true)
    public List<CatalogView> sellerCatalogSearch(String search) {
        String s = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return catalogs.findAll((r, q, cb) -> cb.and(cb.equal(r.get("moderationStatus"), CatalogModerationStatus.APPROVED), cb.isTrue(r.get("active")), s.isEmpty() ? cb.conjunction() : cb.or(cb.like(cb.lower(r.get("name")), "%" + s + "%"), cb.like(cb.lower(r.get("brand")), "%" + s + "%"), cb.like(cb.lower(r.get("oemNumber")), "%" + s + "%"))), PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"))).stream().map(c -> catalogView(c, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public Page<CatalogView> adminCatalogs(CatalogModerationStatus status, int page, int size) {
        Page<CatalogProduct> p = catalogs.findAll((r, q, cb) -> status == null ? cb.conjunction() : cb.equal(r.get("moderationStatus"), status), PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return p.map(c -> catalogView(c, List.of()));
    }

    @Transactional(readOnly = true)
    public CatalogView adminCatalog(Long id) {
        CatalogProduct c = catalogs.findById(id).orElseThrow(() -> new IllegalArgumentException("Catalog product not found."));
        return catalogView(c, List.of());
    }

    @Transactional
    public OfferView setOfferActive(Long id, boolean active) {
        ProductOffer o = offers.findById(id).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı."));
        o.setActive(active);
        return view(o);
    }

    @Transactional
    public OfferView reviewOffer(Long id, boolean approve, String reason) {
        ProductOffer o = offers.findById(id).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı."));
        if (o.getModerationStatus() != ProductModerationStatus.PENDING)
            throw new IllegalArgumentException("Yalnızca bekleyen ilanlar sonuçlandırılabilir.");
        if (!approve && (reason == null || reason.isBlank()))
            throw new IllegalArgumentException("Ret gerekçesi zorunludur.");
        o.setModerationStatus(approve ? ProductModerationStatus.APPROVED : ProductModerationStatus.REJECTED);
        o.setRejectionReason(approve ? null : reason.trim());
        o.setReviewedAt(LocalDateTime.now());
        return view(o);
    }

    @Transactional
    public CatalogProduct reviewCatalog(Long id, boolean approve, String reason) {
        CatalogProduct c = catalogs.findById(id).orElseThrow(() -> new IllegalArgumentException("Katalog ürünü bulunamadı."));
        if (c.getModerationStatus() != CatalogModerationStatus.PENDING)
            throw new IllegalArgumentException("Yalnızca bekleyen katalog ürünleri sonuçlandırılabilir.");
        if (!approve && (reason == null || reason.isBlank()))
            throw new IllegalArgumentException("Ret gerekçesi zorunludur.");
        c.setModerationStatus(approve ? CatalogModerationStatus.APPROVED : CatalogModerationStatus.REJECTED);
        c.setRejectionReason(approve ? null : reason.trim());
        c.setReviewedAt(LocalDateTime.now());
        return c;
    }

    @Transactional
    public CatalogProduct setCatalogActive(Long id, boolean active) {
        CatalogProduct catalog = catalogs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catalog product not found."));
        catalog.setActive(active);
        return catalog;
    }

    @Transactional
    public void favorite(String email, Long catalogId) {
        User u = currentUser(email);
        CatalogProduct c = catalogs.findById(catalogId).filter(x -> x.getModerationStatus() == CatalogModerationStatus.APPROVED && x.getActive()).orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        if (favorites.findByUserIdAndCatalogProductId(u.getId(), catalogId).isEmpty())
            favorites.save(UserFavorite.builder().user(u).catalogProduct(c).build());
    }

    @Transactional
    public void unfavorite(String email, Long id) {
        User u = currentUser(email);
        favorites.findByUserIdAndCatalogProductId(u.getId(), id).ifPresent(favorites::delete);
    }

    @Transactional(readOnly = true)
    public List<CatalogView> myFavorites(String email) {
        User u = currentUser(email);
        return favorites.findByUserIdOrderByCreatedAtDesc(u.getId()).stream().filter(f -> f.getCatalogProduct().getActive() && f.getCatalogProduct().getModerationStatus() == CatalogModerationStatus.APPROVED).map(f -> {
            CatalogProduct c = f.getCatalogProduct();
            List<OfferView> os = offers.findAll(MarketplaceSpecifications.offers(null, null, null, null, null, true), Sort.by(Sort.Direction.ASC, "price")).stream().filter(o -> o.getCatalogProduct().getId().equals(c.getId())).map(this::view).toList();
            return catalogView(c, os);
        }).toList();
    }

    @Transactional
    public ComplaintView createComplaint(String email, ComplaintCreateRequest r) {
        User u = currentUser(email);
        Seller seller = sellers.findById(r.sellerId()).orElseThrow(() -> new IllegalArgumentException("Firma bulunamadı."));
        if (complaints.countByReporterIdAndSellerIdAndCreatedAtAfter(u.getId(), seller.getId(), LocalDateTime.now().minusDays(1)) >= 5)
            throw new IllegalArgumentException("Günlük şikâyet sınırına ulaşıldı.");
        ProductOffer offer = null;
        if (r.offerId() != null) {
            offer = offers.findById(r.offerId()).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı."));
            if (!offer.getSeller().getId().equals(seller.getId()))
                throw new IllegalArgumentException("İlan bu firmaya ait değil.");
            if (complaints.existsByReporterIdAndOfferIdAndCreatedAtAfter(u.getId(), offer.getId(), LocalDateTime.now().minusDays(30)))
                throw new IllegalArgumentException("Bu ilan için yakın zamanda şikâyet gönderdiniz.");
        }
        Complaint saved = complaints.save(Complaint.builder().reporter(u).seller(seller).offer(offer).category(r.category()).description(r.description().trim()).build());
        return complaint(saved);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintView> adminComplaints(ComplaintStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return complaints.findAll((root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status), pageable).map(this::complaint);
    }

    @Transactional
    public ComplaintView resolveComplaint(Long id, ComplaintStatus status, String note) {
        if (status == ComplaintStatus.OPEN) throw new IllegalArgumentException("Sonuç durumu geçersiz.");
        Complaint c = complaints.findById(id).orElseThrow(() -> new IllegalArgumentException("Şikâyet bulunamadı."));
        c.setStatus(status);
        c.setAdminNote(blank(note));
        c.setReviewedAt(LocalDateTime.now());
        return complaint(c);
    }

    @Transactional
    public String addPhoto(String email, Long offerId, MultipartFile file) {
        Seller s = approvedSeller(email);
        ProductOffer o = offers.findByIdAndSellerId(offerId, s.getId()).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı."));
        try {
            if (o.getPhotos().size() >= 8)
                throw new IllegalArgumentException("Bir ilana en fazla 8 fotoğraf eklenebilir.");
            if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
                throw new IllegalArgumentException("Görsel 5 MB boyut sınırını aşmamalıdır.");
            byte[] bytes = file.getBytes();
            String type = detect(bytes);
            validateImage(bytes, type);
            String ext = type.equals("image/jpeg") ? "jpg" : type.substring(6);
            String key = storage.newKey(ext);
            String url = storage.put(key, type, bytes);
            int order = o.getPhotos().size();
            if (o.getModerationStatus() == ProductModerationStatus.APPROVED) {
                o.setModerationStatus(ProductModerationStatus.PENDING);
                o.setReviewedAt(null);
            }
            OfferPhoto photo = OfferPhoto.builder().offer(o).objectKey(key).publicUrl(url).displayOrder(order).primaryPhoto(order == 0).build();
            o.getPhotos().add(photo);
            return url;
        } catch (IOException e) {
            throw new IllegalArgumentException("Görsel okunamadı.");
        }
    }

    @Transactional
    public void reorderPhotos(String email, Long offerId, List<Long> ids) {
        Seller s = approvedSeller(email);
        ProductOffer o = offers.findByIdAndSellerId(offerId, s.getId()).orElseThrow(() -> new IllegalArgumentException("İlan bulunamadı."));
        List<OfferPhoto> current = photos.findByOfferIdOrderByDisplayOrder(offerId);
        if (ids.size() != current.size() || new HashSet<>(ids).size() != ids.size() || !current.stream().map(OfferPhoto::getId).collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(ids)))
            throw new IllegalArgumentException("Fotoğraf sıralaması geçersiz.");
        for (int i = 0; i < ids.size(); i++) {
            Long pid = ids.get(i);
            OfferPhoto ph = current.stream().filter(x -> x.getId().equals(pid)).findFirst().orElseThrow();
            ph.setDisplayOrder(i);
            ph.setPrimaryPhoto(i == 0);
        }
    }

    @Transactional
    public void deletePhoto(String email, Long photoId) {
        Seller s = approvedSeller(email);
        OfferPhoto p = photos.findByIdAndOfferSellerId(photoId, s.getId()).orElseThrow(() -> new IllegalArgumentException("Görsel bulunamadı."));
        String key = p.getObjectKey();
        ProductOffer o = p.getOffer();
        if (o.getModerationStatus() == ProductModerationStatus.APPROVED) {
            o.setModerationStatus(ProductModerationStatus.PENDING);
            o.setReviewedAt(null);
        }
        boolean primary = Boolean.TRUE.equals(p.getPrimaryPhoto());
        photos.delete(p);
        o.getPhotos().remove(p);
        if (primary && !o.getPhotos().isEmpty()) o.getPhotos().get(0).setPrimaryPhoto(true);
        storage.delete(key);
    }

    private String detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) return "image/jpeg";
        if (b.length >= 8 && (b[0] & 255) == 137 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10)
            return "image/png";
        if (b.length >= 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP"))
            return "image/webp";
        throw new IllegalArgumentException("Yalnızca gerçek JPG, PNG veya WebP görselleri kabul edilir.");
    }

    private void validateImage(byte[] b, String type) {
        try {
            if (type.equals("image/jpeg") || type.equals("image/png")) {
                java.awt.image.BufferedImage image = ImageIO.read(new ByteArrayInputStream(b));
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1 || (long) image.getWidth() * image.getHeight() > 40000000L)
                    throw new IllegalArgumentException("Görsel içeriği geçersiz veya çözünürlüğü çok yüksek.");
            } else {
                int declared = (b[4] & 255) | ((b[5] & 255) << 8) | ((b[6] & 255) << 16) | ((b[7] & 255) << 24);
                String chunk = new String(b, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (b.length < 30 || declared != b.length - 8 || !(chunk.equals("VP8 ") || chunk.equals("VP8L") || chunk.equals("VP8X")))
                    throw new IllegalArgumentException("WebP görsel içeriği geçersiz.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Görsel içeriği çözümlenemedi.");
        }
    }

    private void replaceCompatibility(CatalogProduct c, List<ProductVehicleCompatibilityRequest> rs) {
        if (rs == null) return;
        Set<String> seen = new HashSet<>();
        for (var r : rs) {
            if (r.getYearFrom() == null || r.getYearTo() == null || r.getYearFrom() > r.getYearTo())
                throw new IllegalArgumentException("Araç yılı geçersiz.");
            String k = (r.getVehicleBrand() + "|" + r.getVehicleModel() + "|" + r.getYearFrom() + "|" + r.getYearTo() + "|" + r.getEngineInfo()).toLowerCase(Locale.ROOT);
            if (!seen.add(k)) throw new IllegalArgumentException("Tekrarlı araç uyumluluğu.");
            c.getVehicleCompatibilities().add(ProductVehicleCompatibility.builder().catalogProduct(c).vehicleBrand(r.getVehicleBrand().trim()).vehicleModel(r.getVehicleModel().trim()).yearFrom(r.getYearFrom()).yearTo(r.getYearTo()).engineInfo(r.getEngineInfo()).build());
        }
    }

    private OfferView view(ProductOffer o) {
        CatalogProduct c = o.getCatalogProduct();
        Seller s = o.getSeller();
        return new OfferView(o.getId(), c.getId(), c.getName(), c.getDescription(), o.getOfferDescription(), c.getCategory(), c.getBrand(), c.getOemNumber(), o.getPrice(), o.getStockQuantity(), o.getCondition(), o.getModerationStatus(), o.getRejectionReason(), o.getActive(), o.getPriceUpdatedAt(), o.getStockUpdatedAt(), o.getLegacyProductId() == null ? null : legacyImage(o.getLegacyProductId()), o.getPhotos().stream().map(x -> new OfferPhotoView(x.getId(), storage.publicUrl(x.getObjectKey(), x.getPublicUrl()), x.getDisplayOrder(), x.getPrimaryPhoto())).toList(), s.getId(), s.getStoreName(), s.getPhone(), s.getWhatsappPhone(), s.getCity(), s.getDistrict(), s.getLogoUrl(), s.getLatitude(), s.getLongitude(), s.getStreetAddress(), o.getCreatedAt());
    }

    private String legacyImage(Long id) {
        return legacyProducts.findById(id).map(Product::getImageUrl).orElse(null);
    }

    private CatalogView catalogView(CatalogProduct c, List<OfferView> os) {
        return new CatalogView(c.getId(), c.getName(), c.getDescription(), c.getCategory(), c.getBrand(), c.getOemNumber(), c.getModerationStatus().name(), c.getActive(), c.getVehicleCompatibilities().stream().map(v -> ProductVehicleCompatibilityResponse.builder().id(v.getId()).vehicleBrand(v.getVehicleBrand()).vehicleModel(v.getVehicleModel()).yearFrom(v.getYearFrom()).yearTo(v.getYearTo()).engineInfo(v.getEngineInfo()).build()).toList(), os);
    }

    private ComplaintView complaint(Complaint c) {
        return new ComplaintView(c.getId(), c.getReporter().getId(), c.getSeller().getId(), c.getSeller().getStoreName(), c.getOffer() == null ? null : c.getOffer().getId(), c.getCategory(), c.getDescription(), c.getStatus(), c.getAdminNote(), c.getCreatedAt(), c.getReviewedAt());
    }

    private String blank(String x) {
        return x == null || x.isBlank() ? null : x.trim();
    }
}
