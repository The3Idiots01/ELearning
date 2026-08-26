package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** Chuyển lesson sang section khác (hoặc đổi vị trí trong cùng section). */
@Data
public class MoveLessonRequest {

    @NotNull(message = "targetSectionId is required")
    private Long targetSectionId;

    /** Vị trí chèn mong muốn (0-based). Vượt quá sẽ được kẹp về cuối. */
    @NotNull(message = "position is required")
    @PositiveOrZero(message = "position must be >= 0")
    private Integer position;
}
