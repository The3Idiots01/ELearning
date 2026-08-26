package com.learnova.elearning.module.course.exception;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import lombok.Getter;

import java.util.List;

/**
 * Ném khi publish nhưng course chưa đạt điều kiện. Mang theo danh sách issue để
 * handler đính vào ProblemDetail cho FE hiển thị checklist còn thiếu.
 */
@Getter
public class CourseNotReadyException extends AppException {

    private final transient List<PublishIssue> issues;

    public CourseNotReadyException(List<PublishIssue> issues) {
        super(ErrorCode.COURSE_NOT_READY_TO_PUBLISH);
        this.issues = issues;
    }
}
