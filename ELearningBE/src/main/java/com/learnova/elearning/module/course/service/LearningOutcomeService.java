package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.CreateLearningOutcomeRequest;
import com.learnova.elearning.module.course.dto.request.UpdateLearningOutcomeRequest;
import com.learnova.elearning.module.course.dto.response.LearningOutcomeResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.mapper.LearningOutcomeMapper;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.course.exception.CourseNotReadyException;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningOutcomeService {
    private CoursePublishValidator publishValidator;

    @Autowired
    void setPublishValidator(CoursePublishValidator publishValidator) {
        this.publishValidator = publishValidator;
    }

    private final CourseOwnershipGuard ownershipGuard;
    private final LearningOutcomeRepository outcomeRepository;

    @Transactional(readOnly = true)
    public List<LearningOutcomeResponse> list(Long courseId, Long userId) {
        ownershipGuard.requireOwnedCourse(courseId, userId);
        return outcomeRepository.findByCourse_IdOrderByPositionAsc(courseId).stream()
                .map(LearningOutcomeMapper::toResponse)
                .toList();
    }

    @Transactional
    public LearningOutcomeResponse create(Long courseId, CreateLearningOutcomeRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        LearningOutcome outcome = LearningOutcome.builder()
                .course(course)
                .statement(request.getStatement().trim())
                .position((int) outcomeRepository.countByCourse_Id(courseId))
                .build();
        LearningOutcomeResponse response = LearningOutcomeMapper.toResponse(outcomeRepository.save(outcome));
        validateLiveCourse(course);
        return response;
    }

    @Transactional
    public LearningOutcomeResponse update(Long courseId, Long outcomeId,
                                          UpdateLearningOutcomeRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        LearningOutcome outcome = ownershipGuard.requireOutcomeInCourse(outcomeId, courseId);
        outcome.setStatement(request.getStatement().trim());
        LearningOutcomeResponse response = LearningOutcomeMapper.toResponse(outcomeRepository.save(outcome));
        validateLiveCourse(course);
        return response;
    }

    @Transactional
    public void delete(Long courseId, Long outcomeId, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        LearningOutcome outcome = ownershipGuard.requireOutcomeInCourse(outcomeId, courseId);
        if (outcomeRepository.countActiveLessonReferences(outcomeId) > 0
                || outcomeRepository.countActiveAssessmentReferences(outcomeId) > 0) {
            throw new AppException(ErrorCode.LEARNING_OUTCOME_IN_USE);
        }
        outcome.setDeletedAt(Instant.now());
        outcomeRepository.save(outcome);
        normalizePositions(courseId);
        validateLiveCourse(course);
    }

    @Transactional
    public List<LearningOutcomeResponse> reorder(Long courseId, List<Long> outcomeIds, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        List<LearningOutcome> outcomes = outcomeRepository.findByCourse_IdOrderByPositionAsc(courseId);
        validateExactIds(outcomeIds, outcomes.stream().map(LearningOutcome::getId).toList());

        Map<Long, LearningOutcome> byId = outcomes.stream()
                .collect(Collectors.toMap(LearningOutcome::getId, outcome -> outcome));
        for (int i = 0; i < outcomeIds.size(); i++) {
            byId.get(outcomeIds.get(i)).setPosition(i);
        }
        outcomeRepository.saveAll(outcomes);
        return outcomeIds.stream().map(byId::get).map(LearningOutcomeMapper::toResponse).toList();
    }

    private void normalizePositions(Long courseId) {
        List<LearningOutcome> outcomes = outcomeRepository.findByCourse_IdOrderByPositionAsc(courseId);
        for (int i = 0; i < outcomes.size(); i++) {
            outcomes.get(i).setPosition(i);
        }
        outcomeRepository.saveAll(outcomes);
    }

    private void validateExactIds(List<Long> submitted, List<Long> existing) {
        if (submitted == null || submitted.size() != existing.size()
                || !new HashSet<>(submitted).equals(new HashSet<>(existing))) {
            throw new AppException(ErrorCode.ORDER_PAYLOAD_MISMATCH);
        }
    }

    private void validateLiveCourse(Course course) {
        if (course != null && course.getStatus() == CourseStatus.PUBLISHED && publishValidator != null) {
            var issues = publishValidator.validateLiveSnapshot(course);
            if (!issues.isEmpty()) throw new CourseNotReadyException(issues);
        }
    }
}
