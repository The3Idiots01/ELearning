-- V15 — Hợp nhất learner/lecturer thành user thường

UPDATE users
SET role = 'USER'
WHERE role IN ('LEARNER', 'LECTURER');

ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));
