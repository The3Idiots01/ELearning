package com.learnova.elearning.module.course.dto.request;

import lombok.Data;

/** Strong confirmation required before hiding a live curriculum item. */
@Data
public class ArchiveConfirmationRequest {
    private Boolean confirm;
}
