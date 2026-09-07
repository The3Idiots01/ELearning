package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import lombok.Getter;

/**
 * {@link ErrorCode#PLAYBACK_TICKET_EXPIRED} kèm {@link PlaybackTicketClaims} —
 * để {@code ContentStreamController} ghi {@code content_access_logs} với
 * {@code lessonId}/{@code jti} trước khi trả lỗi (§4.x). Vẫn là một
 * {@link AppException} với đúng {@code errorCode} như trước; chỉ mang thêm dữ
 * liệu, không đổi hợp đồng lỗi hiện có.
 */
@Getter
public class PlaybackTicketExpiredException extends AppException {

    private final PlaybackTicketClaims claims;

    public PlaybackTicketExpiredException(PlaybackTicketClaims claims) {
        super(ErrorCode.PLAYBACK_TICKET_EXPIRED);
        this.claims = claims;
    }
}
