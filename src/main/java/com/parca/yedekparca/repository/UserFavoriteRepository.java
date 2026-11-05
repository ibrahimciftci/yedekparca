package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    Optional<UserFavorite> findByUserIdAndCatalogProductId(Long userId, Long catalogId);

    List<UserFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
