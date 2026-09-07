package com.learnova.elearning.module.tracking.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageCalculatorTest {

    private final CoverageCalculator calculator = new CoverageCalculator();

    @Test
    void build_typicalRanges_matchesExampleFromDesign() {
        List<List<Double>> ranges = List.of(List.of(0.0, 205.2), List.of(280.0, 430.5));

        String literal = calculator.buildWatchedRangesLiteral(ranges, 612);

        assertThat(literal).isEqualTo("{[0,206),[280,431)}");
    }

    @Test
    void build_nullOrEmptyRanges_returnsEmptyMultirange() {
        assertThat(calculator.buildWatchedRangesLiteral(null, 600)).isEqualTo("{}");
        assertThat(calculator.buildWatchedRangesLiteral(List.of(), 600)).isEqualTo("{}");
    }

    @Test
    void build_durationZero_returnsEmptyMultirange() {
        List<List<Double>> ranges = List.of(List.of(0.0, 50.0));

        assertThat(calculator.buildWatchedRangesLiteral(ranges, 0)).isEqualTo("{}");
    }

    @Test
    void build_negativeDuration_returnsEmptyMultirange() {
        List<List<Double>> ranges = List.of(List.of(0.0, 50.0));

        assertThat(calculator.buildWatchedRangesLiteral(ranges, -5)).isEqualTo("{}");
    }

    @Test
    void build_rangeExceedsDuration_clampsToUpperBound() {
        List<List<Double>> ranges = List.of(List.of(0.0, 999.0));

        String literal = calculator.buildWatchedRangesLiteral(ranges, 100);

        assertThat(literal).isEqualTo("{[0,100)}");
    }

    @Test
    void build_negativeStart_clampsToZero() {
        List<List<Double>> ranges = List.of(List.of(-50.0, 10.0));

        String literal = calculator.buildWatchedRangesLiteral(ranges, 100);

        assertThat(literal).isEqualTo("{[0,10)}");
    }

    @Test
    void build_overlappingRanges_passedThroughAsSeparateEntries() {
        // Postgres tự gộp khoảng chồng lấn khi dựng int4multirange — không phải việc của lớp này.
        List<List<Double>> ranges = List.of(List.of(0.0, 50.0), List.of(20.0, 80.0));

        String literal = calculator.buildWatchedRangesLiteral(ranges, 200);

        assertThat(literal).isEqualTo("{[0,50),[20,80)}");
    }

    @Test
    void build_adjacentRanges_passedThroughAsSeparateEntries() {
        List<List<Double>> ranges = List.of(List.of(0.0, 50.0), List.of(50.0, 80.0));

        String literal = calculator.buildWatchedRangesLiteral(ranges, 200);

        assertThat(literal).isEqualTo("{[0,50),[50,80)}");
    }

    @Test
    void build_lowerGreaterThanOrEqualUpperAfterClamp_droppedSilently() {
        List<List<Double>> ranges = Arrays.asList(
                List.of(50.0, 50.0),   // rỗng
                List.of(80.0, 20.0),   // lower > upper
                List.of(10.0, 30.0)    // hợp lệ
        );

        String literal = calculator.buildWatchedRangesLiteral(ranges, 200);

        assertThat(literal).isEqualTo("{[10,30)}");
    }

    @Test
    void build_malformedEntries_droppedSilently() {
        List<List<Double>> ranges = Arrays.asList(
                null,
                List.of(1.0),
                Arrays.asList(1.0, null),
                List.of(5.0, 15.0)
        );

        String literal = calculator.buildWatchedRangesLiteral(ranges, 200);

        assertThat(literal).isEqualTo("{[5,15)}");
    }
}
