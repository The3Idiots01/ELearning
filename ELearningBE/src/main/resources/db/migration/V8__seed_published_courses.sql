-- =============================================================================
-- V8 — Seed sample published courses and lessons for student discovery testing
-- =============================================================================

ALTER TABLE courses ADD COLUMN IF NOT EXISTS rating_count INT DEFAULT 0;

-- 1. Ensure seed lecturer user exists
INSERT INTO users (id, full_name, email, password_hash, role, auth_provider, is_active, created_at)
VALUES (
    1,
    'Giảng viên Learnova Master',
    'lecturer@learnova.com',
    '$2a$10$7R0Z4/Vz4zJz1u7K8p/X.e1nK9H8Fz5J0l5v6H1g2k3m4n5o6p7q', -- BCrypt dummy hash
    'LECTURER',
    'LOCAL',
    TRUE,
    now()
) ON CONFLICT (email) DO NOTHING;

-- 2. Insert Published Sample Courses
INSERT INTO courses (
    id, lecturer_id, category_id, title, slug, subtitle, description,
    price, level, status, rating_avg, rating_count, total_students, published_at, created_at
) VALUES 
(
    101, 1, 1,
    'Lập trình React 19 & TypeScript Từ Cơ Bản Đến Nâng Cao',
    'lap-trinh-react-19-typescript-tu-co-ban-den-nang-cao',
    'Thành thạo React 19, Custom Hooks, Redux Toolkit và xây dựng ứng dụng thực chiến.',
    'Khóa học trang bị cho bạn kiến thức chuyên sâu về React 19 mới nhất, kết hợp TypeScript để viết code sạch và tối ưu hiệu năng.',
    499000, 'INTERMEDIATE', 'PUBLISHED', 4.9, 128, 1420, now(), now()
),
(
    102, 1, 1,
    'Spring Boot 3 & PostgreSQL - Xây Dựng Microservices Chuyên Nghiệp',
    'spring-boot-3-postgresql-xay-dung-microservices-chuyen-nghiep',
    'Tạo REST API chuẩn RESTful, tích hợp Spring Security JWT, JPA Hibernate và Docker.',
    'Học cách thiết kế hệ thống Backend quy mô lớn với Spring Boot 3, tối ưu truy vấn PostgreSQL và triển khai lên môi trường sản xuất.',
    799000, 'ADVANCED', 'PUBLISHED', 4.8, 95, 890, now(), now()
),
(
    103, 1, 4,
    'Thiết Kế Giao Diện UI/UX Với Figma Dành Cho Người Mới Bắt Đầu',
    'thiet-ke-giao-dien-ui-ux-voi-figma-danh-cho-nguoi-moi-bat-dau',
    'Tạo Prototype, Design System và Wireframe ấn tượng đạt chuẩn thiết kế quốc tế.',
    'Khóa học thực hành thiết kế UI/UX từ con số 0. Tự tay thiết kế ứng dụng di động và website chuyên nghiệp với Figma.',
    0, 'BEGINNER', 'PUBLISHED', 5.0, 210, 2300, now(), now()
)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence for courses table if needed
SELECT setval(pg_get_serial_sequence('courses', 'id'), coalesce(max(id), 1)) FROM courses;

-- 3. Insert Sections for Course 101
INSERT INTO course_sections (id, course_id, title, position, created_at) VALUES
(1001, 101, 'Chương 1: Giới thiệu & Tổng quan về React 19', 1, now()),
(1002, 101, 'Chương 2: Hooks nâng cao & Tối ưu State', 2, now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('course_sections', 'id'), coalesce(max(id), 1)) FROM course_sections;

-- 4. Insert Lessons for Sections
INSERT INTO lessons (
    id, section_id, title, content_type, content_text, duration_seconds, is_preview, upload_status, position, created_at
) VALUES
(
    2001, 1001, 'Bài 1: Giới thiệu về React 19 và các tính năng mới',
    'VIDEO', 'Nội dung video giới thiệu React 19', 420, TRUE, 'READY', 1, now()
),
(
    2002, 1001, 'Bài 2: Cấu hình dự án với Vite và TypeScript',
    'VIDEO', 'Nội dung hướng dẫn khởi tạo dự án', 600, TRUE, 'READY', 2, now()
),
(
    2003, 1002, 'Bài 3: Xây dựng Custom Hooks và State Management',
    'ARTICLE', 'Tài liệu hướng dẫn chi tiết về Custom Hooks trong React', 0, FALSE, 'READY', 1, now()
)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('lessons', 'id'), coalesce(max(id), 1)) FROM lessons;
