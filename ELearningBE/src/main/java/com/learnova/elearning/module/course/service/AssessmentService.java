package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.CreateAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.PlaceAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.UpdateAssessmentRequest;
import com.learnova.elearning.module.course.dto.response.AssessmentResponse;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.mapper.AssessmentMapper;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final CourseOwnershipGuard ownershipGuard;
    private final AssessmentRepository assessmentRepository;
    private final LearningOutcomeRepository outcomeRepository;

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(Long courseId, Long userId) {
        ownershipGuard.requireOwnedCourse(courseId, userId);
        return assessmentRepository.findByCourse_Id(courseId).stream()
                .sorted(Comparator
                        .comparing((Assessment a) -> a.getSection() == null ? 0 : 1)
                        .thenComparing(a -> a.getSection() == null ? 0 : a.getSection().getPosition())
                        .thenComparing(Assessment::getPosition)
                        .thenComparing(Assessment::getId))
                .map(AssessmentMapper::toResponse)
                .toList();
    }

    @Transactional
    public AssessmentResponse create(Long courseId, CreateAssessmentRequest request, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        if (request.getType() != AssessmentType.QUIZ) {
            throw new AppException(ErrorCode.ASSESSMENT_TYPE_UNSUPPORTED);
        }
        Set<LearningOutcome> outcomes = resolveOutcomes(courseId, request.getOutcomeIds());
        Assessment assessment = Assessment.builder()
                .course(course)
                .type(request.getType())
                .title(request.getTitle().trim())
                .instructions(trimToNull(request.getInstructions()))
                .position(0)
                .outcomes(outcomes)
                .build();
        return AssessmentMapper.toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentResponse update(Long courseId, Long assessmentId,
                                     UpdateAssessmentRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Assessment assessment = ownershipGuard.requireAssessmentInCourse(assessmentId, courseId);
        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "title must not be blank");
            }
            assessment.setTitle(title);
        }
        if (request.getInstructions() != null) {
            assessment.setInstructions(trimToNull(request.getInstructions()));
        }
        if (request.getOutcomeIds() != null) {
            Set<LearningOutcome> outcomes = resolveOutcomes(courseId, request.getOutcomeIds());
            assessment.setOutcomes(outcomes);
        }
        return AssessmentMapper.toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentResponse place(Long courseId, Long assessmentId,
                                    PlaceAssessmentRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Assessment assessment = ownershipGuard.requireAssessmentInCourse(assessmentId, courseId);
        Long oldSectionId = assessment.getSection() != null ? assessment.getSection().getId() : null;

        if (request.getSectionId() == null) {
            assessment.setSection(null);
            assessment.setPosition(0);
            assessmentRepository.save(assessment);
            if (oldSectionId != null) {
                normalizeSectionPositions(oldSectionId);
            }
            return AssessmentMapper.toResponse(assessment);
        }

        CourseSection target = ownershipGuard.requireSectionInCourse(request.getSectionId(), courseId);
        List<Assessment> targetAssessments = new ArrayList<>(
                assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(target.getId()));
        targetAssessments.removeIf(item -> item.getId().equals(assessmentId));
        int insertAt = Math.min(request.getPosition(), targetAssessments.size());

        assessment.setSection(target);
        targetAssessments.add(insertAt, assessment);
        for (int i = 0; i < targetAssessments.size(); i++) {
            targetAssessments.get(i).setPosition(i);
        }
        assessmentRepository.saveAll(targetAssessments);

        if (oldSectionId != null && !oldSectionId.equals(target.getId())) {
            normalizeSectionPositions(oldSectionId);
        }
        return AssessmentMapper.toResponse(assessment);
    }

    @Transactional
    public List<AssessmentResponse> reorder(Long courseId, Long sectionId,
                                            List<Long> assessmentIds, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        ownershipGuard.requireSectionInCourse(sectionId, courseId);
        List<Assessment> assessments = assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(sectionId);
        validateExactIds(assessmentIds, assessments.stream().map(Assessment::getId).toList());

        Map<Long, Assessment> byId = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId, item -> item));
        for (int i = 0; i < assessmentIds.size(); i++) {
            byId.get(assessmentIds.get(i)).setPosition(i);
        }
        assessmentRepository.saveAll(assessments);
        return assessmentIds.stream().map(byId::get).map(AssessmentMapper::toResponse).toList();
    }

    @Transactional
    public void delete(Long courseId, Long assessmentId, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Assessment assessment = ownershipGuard.requireAssessmentInCourse(assessmentId, courseId);
        Long sectionId = assessment.getSection() != null ? assessment.getSection().getId() : null;
        assessment.setDeletedAt(Instant.now());
        assessmentRepository.save(assessment);
        if (sectionId != null) {
            normalizeSectionPositions(sectionId);
        }
    }

    /** Called from section deletion so assessment plans survive curriculum restructuring. */
    @Transactional
    public void unassignFromSection(Long sectionId) {
        List<Assessment> assessments = assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(sectionId);
        for (Assessment assessment : assessments) {
            assessment.setSection(null);
            assessment.setPosition(0);
        }
        assessmentRepository.saveAll(assessments);
    }

    private Set<LearningOutcome> resolveOutcomes(Long courseId, List<Long> outcomeIds) {
        if (outcomeIds == null) {
            throw new AppException(ErrorCode.ASSESSMENT_OUTCOME_MISMATCH);
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(outcomeIds);
        if (uniqueIds.size() != outcomeIds.size()) {
            throw new AppException(ErrorCode.ASSESSMENT_OUTCOME_MISMATCH,
                    "outcomeIds must not contain duplicates");
        }
        if (uniqueIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<LearningOutcome> outcomes = outcomeRepository.findByCourse_IdAndIdIn(courseId, uniqueIds);
        if (outcomes.size() != uniqueIds.size()) {
            throw new AppException(ErrorCode.ASSESSMENT_OUTCOME_MISMATCH);
        }
        Map<Long, LearningOutcome> byId = outcomes.stream()
                .collect(Collectors.toMap(LearningOutcome::getId, outcome -> outcome));
        return uniqueIds.stream().map(byId::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void normalizeSectionPositions(Long sectionId) {
        List<Assessment> assessments = assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(sectionId);
        for (int i = 0; i < assessments.size(); i++) {
            assessments.get(i).setPosition(i);
        }
        assessmentRepository.saveAll(assessments);
    }

    private void validateExactIds(List<Long> submitted, List<Long> existing) {
        if (submitted == null || submitted.size() != existing.size()
                || !new HashSet<>(submitted).equals(new HashSet<>(existing))) {
            throw new AppException(ErrorCode.ORDER_PAYLOAD_MISMATCH);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
