package com.learnova.elearning.module.qa.service;

import com.learnova.elearning.module.qa.dto.request.CreateAnswerRequest;
import com.learnova.elearning.module.qa.dto.request.CreateQuestionRequest;
import com.learnova.elearning.module.qa.dto.response.CourseAnswerResponse;
import com.learnova.elearning.module.qa.dto.response.CourseQuestionResponse;

import java.util.List;

public interface QaService {

    List<CourseQuestionResponse> getQuestionsByLesson(Long courseId, Long lessonId);

    List<CourseQuestionResponse> getQuestionsByCourse(Long courseId);

    CourseQuestionResponse createQuestion(Long courseId, Long lessonId, CreateQuestionRequest request, Long currentUserId);

    CourseAnswerResponse createAnswer(Long courseId, Long questionId, CreateAnswerRequest request, Long currentUserId);

    void deleteQuestion(Long courseId, Long questionId, Long currentUserId);

    void deleteAnswer(Long courseId, Long questionId, Long answerId, Long currentUserId);

    List<CourseQuestionResponse> getInstructorCourseQuestions(Long courseId, String filter, Long lecturerId);
}
