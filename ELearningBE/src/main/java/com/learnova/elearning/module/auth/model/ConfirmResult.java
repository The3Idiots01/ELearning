package com.learnova.elearning.module.auth.model;

/**
 * Kết quả nghiệp vụ của luồng xác nhận tài khoản (confirm-account).
 * Service chỉ trả về kết quả này — việc render trang HTML do tầng controller/view đảm nhiệm.
 */
public record ConfirmResult(
        boolean success,
        String message,
        String displayName,
        RedirectTarget target
) {

    /**
     * Đích mà nút bấm trên trang kết quả nên trỏ tới (controller ánh xạ ra URL thật).
     */
    public enum RedirectTarget {
        LOGIN,
        REGISTER
    }

    public static ConfirmResult success(String fullName) {
        return new ConfirmResult(true, null, fullName, RedirectTarget.LOGIN);
    }

    public static ConfirmResult failure(String message, RedirectTarget target) {
        return new ConfirmResult(false, message, null, target);
    }
}
