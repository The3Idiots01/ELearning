package com.learnova.elearning.module.tracking.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dựng literal {@code int4multirange} từ {@code playedRanges} thô của client —
 * §5.2 / §5.6 design_us15_us17.md. Việc gộp khoảng chồng lấn/liền kề là tính
 * chất sẵn có của kiểu {@code int4multirange} trong PostgreSQL khi constructor
 * dựng giá trị từ nhiều {@code int4range} — không cần merge tay ở tầng Java;
 * lớp này chỉ chịu trách nhiệm lọc/khớp biên trước khi giao cho Postgres.
 */
@Component
public class CoverageCalculator {

    private static final String EMPTY_MULTIRANGE = "{}";

    /**
     * @param playedRanges mỗi phần tử là {@code [start, end]} giây (như {@code video.played})
     * @param durationSeconds thời lượng thật của lesson (server đo, KHÔNG phải client khai)
     * @return literal {@code int4multirange}, ví dụ {@code "{[0,205),[280,431)}"}, hoặc {@code "{}"}
     */
    public String buildWatchedRangesLiteral(List<List<Double>> playedRanges, int durationSeconds) {
        return literalOf(parseAndClamp(playedRanges, durationSeconds));
    }

    /**
     * Lọc/khớp biên {@code playedRanges} thô về danh sách khoảng nguyên hợp lệ
     * trong {@code [0, durationSeconds]} — cùng quy tắc loại bỏ như §5.6: khoảng
     * dị dạng hoặc {@code lower >= upper} sau khi kẹp biên bị loại im lặng.
     */
    public List<int[]> parseAndClamp(List<List<Double>> playedRanges, int durationSeconds) {
        List<int[]> result = new ArrayList<>();
        if (playedRanges == null || playedRanges.isEmpty() || durationSeconds <= 0) {
            return result;
        }
        for (List<Double> range : playedRanges) {
            if (range == null || range.size() != 2 || range.get(0) == null || range.get(1) == null) {
                continue; // khoảng dị dạng — loại bỏ im lặng (§5.6)
            }
            long start = clamp(Math.floor(range.get(0)), durationSeconds);
            long end = clamp(Math.ceil(range.get(1)), durationSeconds);
            if (end <= start) {
                continue; // lower >= upper sau khi kẹp biên — loại bỏ im lặng (§5.6)
            }
            result.add(new int[]{(int) start, (int) end});
        }
        return result;
    }

    /** @return literal {@code int4multirange} dựng từ danh sách khoảng {@code [start,end)} đã lọc. */
    public String literalOf(List<int[]> ranges) {
        if (ranges.isEmpty()) {
            return EMPTY_MULTIRANGE;
        }
        StringBuilder literal = new StringBuilder("{");
        boolean first = true;
        for (int[] range : ranges) {
            if (!first) {
                literal.append(',');
            }
            literal.append('[').append(range[0]).append(',').append(range[1]).append(')');
            first = false;
        }
        literal.append('}');
        return literal.toString();
    }

    /**
     * Chống gian lận (§5.6): khi lượng coverage đề nghị vượt hạn mức
     * {@code budgetSeconds} của {@link HeartbeatRateLimiter}, chỉ nhận phần
     * khoảng gần {@code position} nhất cho đủ hạn mức thay vì từ chối toàn bộ
     * heartbeat. Xử lý theo thứ tự gần {@code position} nhất trước — mỗi khoảng
     * được nhận trọn nếu còn đủ ngân sách, hoặc cắt về phía gần {@code position}
     * nếu ngân sách chỉ còn một phần. Đây là một xấp xỉ có chủ đích (không phải
     * thuật toán tối ưu tuyệt đối) — mức phòng thủ hợp lý theo đúng tinh thần
     * "không chống gian lận tuyệt đối" của §5.6.
     */
    public List<int[]> truncateNearPosition(List<int[]> ranges, int position, int budgetSeconds) {
        List<int[]> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(r -> distanceToPosition(r, position)));

        List<int[]> accepted = new ArrayList<>();
        int remaining = Math.max(0, budgetSeconds);
        for (int[] range : sorted) {
            if (remaining <= 0) {
                break;
            }
            int length = range[1] - range[0];
            if (length <= remaining) {
                accepted.add(range);
                remaining -= length;
                continue;
            }
            // Ngân sách chỉ còn một phần — cắt về phía gần position nhất.
            if (position <= range[0]) {
                accepted.add(new int[]{range[0], range[0] + remaining});
            } else if (position >= range[1]) {
                accepted.add(new int[]{range[1] - remaining, range[1]});
            } else {
                int half = remaining / 2;
                int start = Math.max(range[0], position - half);
                int end = Math.min(range[1], start + remaining);
                start = Math.max(range[0], end - remaining);
                accepted.add(new int[]{start, end});
            }
            remaining = 0;
        }
        return accepted;
    }

    private int distanceToPosition(int[] range, int position) {
        if (position >= range[0] && position <= range[1]) {
            return 0;
        }
        return Math.min(Math.abs(position - range[0]), Math.abs(position - range[1]));
    }

    private long clamp(double value, int durationSeconds) {
        long rounded = (long) value;
        return Math.max(0, Math.min(rounded, durationSeconds));
    }
}
