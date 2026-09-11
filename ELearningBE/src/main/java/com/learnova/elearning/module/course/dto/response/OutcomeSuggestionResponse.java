package com.learnova.elearning.module.course.dto.response;

import java.util.List;

public record OutcomeSuggestionResponse(List<OutcomeSuggestion> suggestions, String warning) {
}
