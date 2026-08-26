package com.learnova.elearning.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Sinh slug thân thiện URL từ tiêu đề tiếng Việt: bỏ dấu, lowercase, thay ký tự
 * ngoài [a-z0-9] bằng '-', gom '-' liên tiếp, cắt độ dài tối đa.
 * Việc đảm bảo duy nhất (thêm hậu tố -2, -3...) do tầng service xử lý.
 */
public final class SlugGenerator {

    private static final int MAX_LENGTH = 200;

    private SlugGenerator() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");

        if (normalized.length() > MAX_LENGTH) {
            normalized = normalized.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return normalized;
    }
}
