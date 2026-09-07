-- V16 — Tối ưu tổng hợp doanh thu theo khóa học

CREATE INDEX IF NOT EXISTS idx_payment_orders_paid_course
    ON payment_orders (course_id)
    WHERE status = 'PAID';
