package com.learnova.elearning.module.delivery.service;

/** Cơ sở cấp quyền của một PlaybackTicket — §4.3 / §4.4 design_us15_us17.md. */
public enum AccessScope {
    PREVIEW,
    ENROLLED,
    OWNER,
    ADMIN
}
