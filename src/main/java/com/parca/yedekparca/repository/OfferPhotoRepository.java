package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface OfferPhotoRepository extends JpaRepository<OfferPhoto, Long> {
    List<OfferPhoto> findByOfferIdOrderByDisplayOrder(Long offerId);

    Optional<OfferPhoto> findByIdAndOfferSellerId(Long id, Long sellerId);

    boolean existsByOfferIdAndPrimaryPhotoTrue(Long offerId);
}
