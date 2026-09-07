package com.learnova.elearning.module.delivery.event;

/** Kết quả một lần đối chiếu quyền tại gateway — cột {@code outcome} của {@code content_access_logs}. */
public enum ContentAccessOutcome {
    GRANTED,
    TICKET_EXPIRED,
    TICKET_MISMATCH,
    ACCESS_DENIED
}
