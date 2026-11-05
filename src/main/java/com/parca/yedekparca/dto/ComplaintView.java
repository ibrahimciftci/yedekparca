package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.*;

import java.time.LocalDateTime;

public record ComplaintView(Long id, Long reporterId, Long sellerId, String sellerName, Long offerId,
                            ComplaintCategory category, String description, ComplaintStatus status, String adminNote,
                            LocalDateTime createdAt, LocalDateTime reviewedAt) {
}
