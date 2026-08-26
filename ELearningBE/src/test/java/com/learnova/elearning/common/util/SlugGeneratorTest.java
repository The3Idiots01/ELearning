package com.learnova.elearning.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

    @Test
    @DisplayName("Slug: bỏ dấu tiếng Việt và lowercase")
    void toSlug_removesVietnameseDiacritics() {
        assertThat(SlugGenerator.toSlug("Java Cơ Bản")).isEqualTo("java-co-ban");
        assertThat(SlugGenerator.toSlug("Lập Trình Web với Spring Boot"))
                .isEqualTo("lap-trinh-web-voi-spring-boot");
        assertThat(SlugGenerator.toSlug("Đường tới thành công")).isEqualTo("duong-toi-thanh-cong");
    }

    @Test
    @DisplayName("Slug: thay ký tự đặc biệt bằng '-' và gom '-' liên tiếp")
    void toSlug_collapsesSpecialChars() {
        assertThat(SlugGenerator.toSlug("C++ & Java: 101!!!")).isEqualTo("c-java-101");
        assertThat(SlugGenerator.toSlug("  nhiều    khoảng   trắng  ")).isEqualTo("nhieu-khoang-trang");
    }

    @Test
    @DisplayName("Slug: chuỗi rỗng hoặc toàn ký tự đặc biệt trả về rỗng")
    void toSlug_blankOrSymbolsOnly() {
        assertThat(SlugGenerator.toSlug(null)).isEmpty();
        assertThat(SlugGenerator.toSlug("   ")).isEmpty();
        assertThat(SlugGenerator.toSlug("!!!@@@###")).isEmpty();
    }

    @Test
    @DisplayName("Slug: cắt tối đa 200 ký tự và không để lại '-' ở cuối")
    void toSlug_truncatesToMaxLength() {
        String longTitle = "a".repeat(300);
        String slug = SlugGenerator.toSlug(longTitle);
        assertThat(slug.length()).isLessThanOrEqualTo(200);
        assertThat(slug).doesNotEndWith("-");
    }
}
