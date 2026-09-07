package com.learnova.elearning.module.delivery.service;

/** Ticket vừa phát hành kèm {@code jti} — dùng để ghi nhận giới hạn phiên đồng thời (§4.9). */
public record PlaybackTicketIssued(String ticket, String jti) {
}
