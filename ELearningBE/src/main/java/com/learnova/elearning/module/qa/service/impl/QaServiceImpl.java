package com.learnova.elearning.module.qa.service.impl;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.qa.dto.request.CreateAnswerRequest;
import com.learnova.elearning.module.qa.dto.request.CreateQuestionRequest;
import com.learnova.elearning.module.qa.dto.response.CourseAnswerResponse;
import com.learnova.elearning.module.qa.dto.response.CourseQuestionResponse;
import com.learnova.elearning.module.qa.dto.response.QuestionAuthorResponse;
import com.learnova.elearning.module.qa.entity.CourseAnswer;
import com.learnova.elearning.module.qa.entity.CourseQuestion;
import com.learnova.elearning.module.qa.repository.CourseAnswerRepository;
import com.learnova.elearning.module.qa.repository.CourseQuestionRepository;
import com.learnova.elearning.module.qa.service.QaService;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    private final CourseQuestionRepository questionRepository;
    private final CourseAnswerRepository answerRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseQuestionResponse> getQuestionsByLesson(Long courseId, Long lessonId) {
        if (!courseRepository.existsById(courseId)) {
            throw new AppException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!lessonRepository.existsById(lessonId)) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }

        List<CourseQuestion> questions = questionRepository.findByCourseIdAndLessonIdWithDetails(courseId, lessonId);
        return questions.stream().map(this::mapToQuestionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseQuestionResponse> getQuestionsByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new AppException(ErrorCode.COURSE_NOT_FOUND);
        }

        List<CourseQuestion> questions = questionRepository.findByCourseIdWithDetails(courseId);
        return questions.stream().map(this::mapToQuestionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseQuestionResponse createQuestion(Long courseId, Long lessonId, CreateQuestionRequest request, Long currentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        Lesson lesson = lessonRepository.findByIdAndSection_Course_Id(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Enforce BR-32: Only enrolled learners or course lecturer (or admin) can ask questions
        validateCanParticipateInQa(course, user);

        CourseQuestion question = CourseQuestion.builder()
                .course(course)
                .lesson(lesson)
                .user(user)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .answers(new ArrayList<>())
                .build();

        CourseQuestion saved = questionRepository.save(question);
        log.info("Created QA question id={} for course={} lesson={} by user={}", saved.getId(), courseId, lessonId, currentUserId);

        return mapToQuestionResponse(saved);
    }

    @Override
    @Transactional
    public CourseAnswerResponse createAnswer(Long courseId, Long questionId, CreateAnswerRequest request, Long currentUserId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        if (!question.getCourse().getId().equals(courseId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Enforce BR-32: Only enrolled learners or course lecturer can answer
        validateCanParticipateInQa(question.getCourse(), user);

        boolean isLecturer = question.getCourse().getLecturer().getId().equals(currentUserId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isInstructorReply = isLecturer || isAdmin;

        CourseAnswer answer = CourseAnswer.builder()
                .question(question)
                .user(user)
                .content(request.getContent().trim())
                .isInstructorReply(isInstructorReply)
                .build();

        CourseAnswer saved = answerRepository.save(answer);
        log.info("Created QA answer id={} for question id={} by user={} (isInstructorReply={})",
                saved.getId(), questionId, currentUserId, isInstructorReply);

        return mapToAnswerResponse(saved);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long courseId, Long questionId, Long currentUserId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        if (!question.getCourse().getId().equals(courseId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isAuthor = question.getUser().getId().equals(currentUserId);
        boolean isLecturer = question.getCourse().getLecturer().getId().equals(currentUserId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isAuthor && !isLecturer && !isAdmin) {
            throw new AppException(ErrorCode.QUESTION_ACCESS_DENIED);
        }

        questionRepository.delete(question);
        log.info("Deleted question id={} by user={}", questionId, currentUserId);
    }

    @Override
    @Transactional
    public void deleteAnswer(Long courseId, Long questionId, Long answerId, Long currentUserId) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!answer.getQuestion().getId().equals(questionId) || !answer.getQuestion().getCourse().getId().equals(courseId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isAuthor = answer.getUser().getId().equals(currentUserId);
        boolean isLecturer = answer.getQuestion().getCourse().getLecturer().getId().equals(currentUserId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isAuthor && !isLecturer && !isAdmin) {
            throw new AppException(ErrorCode.QUESTION_ACCESS_DENIED);
        }

        answerRepository.delete(answer);
        log.info("Deleted answer id={} by user={}", answerId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseQuestionResponse> getInstructorCourseQuestions(Long courseId, String filter, Long lecturerId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        User user = userRepository.findById(lecturerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isLecturer = course.getLecturer().getId().equals(lecturerId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isLecturer && !isAdmin) {
            throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        List<CourseQuestion> questions = questionRepository.findByCourseIdWithDetails(courseId);
        List<CourseQuestionResponse> responses = questions.stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());

        if ("unanswered".equalsIgnoreCase(filter)) {
            return responses.stream().filter(q -> !q.isHasInstructorReply()).collect(Collectors.toList());
        } else if ("answered".equalsIgnoreCase(filter)) {
            return responses.stream().filter(CourseQuestionResponse::isHasInstructorReply).collect(Collectors.toList());
        }

        return responses;
    }

    private void validateCanParticipateInQa(Course course, User user) {
        boolean isLecturer = course.getLecturer().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_Id(user.getId(), course.getId());

        if (!isLecturer && !isAdmin && !isEnrolled) {
            throw new AppException(ErrorCode.NOT_ENROLLED);
        }
    }

    private CourseQuestionResponse mapToQuestionResponse(CourseQuestion question) {
        List<CourseAnswer> answers = question.getAnswers() != null ? question.getAnswers() : List.of();
        boolean hasInstructorReply = answers.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsInstructorReply()));

        List<CourseAnswerResponse> answerResponses = answers.stream()
                .sorted(java.util.Comparator.comparing(
                        CourseAnswer::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                ))
                .map(this::mapToAnswerResponse)
                .collect(Collectors.toList());

        return CourseQuestionResponse.builder()
                .id(question.getId())
                .courseId(question.getCourse().getId())
                .lessonId(question.getLesson().getId())
                .lessonTitle(question.getLesson().getTitle())
                .title(question.getTitle())
                .content(question.getContent())
                .author(QuestionAuthorResponse.builder()
                        .id(question.getUser().getId())
                        .fullName(question.getUser().getFullName())
                        .avatarUrl(question.getUser().getAvatarUrl())
                        .role(question.getUser().getRole().name())
                        .build())
                .answersCount(answerResponses.size())
                .hasInstructorReply(hasInstructorReply)
                .answers(answerResponses)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private CourseAnswerResponse mapToAnswerResponse(CourseAnswer answer) {
        return CourseAnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .content(answer.getContent())
                .isInstructorReply(Boolean.TRUE.equals(answer.getIsInstructorReply()))
                .author(QuestionAuthorResponse.builder()
                        .id(answer.getUser().getId())
                        .fullName(answer.getUser().getFullName())
                        .avatarUrl(answer.getUser().getAvatarUrl())
                        .role(answer.getUser().getRole().name())
                        .build())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }
}
