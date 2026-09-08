package com.learnova.elearning.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class BackwardDesignMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void v17BackfillsBackwardDesignDataWithoutRemovingLegacyQuizData() throws Exception {
        migrateToVersion16();
        seedLegacyCourse();

        migrationRunner().migrate();

        try (Connection connection = connection()) {
            assertEquals("17", scalarString(connection,
                    "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1"));

            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM learning_outcomes"));
            assertEquals("Build a REST API", scalarString(connection,
                    "SELECT statement FROM learning_outcomes WHERE course_id = 100 AND position = 0"));

            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM assessments"));
            assertEquals(500, scalarLong(connection,
                    "SELECT assessment_id FROM quizzes WHERE id = 500"));
            assertEquals(100, scalarLong(connection,
                    "SELECT course_id FROM assessments WHERE id = 500"));
            assertEquals(200, scalarLong(connection,
                    "SELECT section_id FROM assessments WHERE id = 500"));
            assertEquals("Legacy quiz", scalarString(connection,
                    "SELECT title FROM assessments WHERE id = 500"));

            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = 500"));
            assertNull(scalarString(connection,
                    "SELECT outcome_id::text FROM quiz_questions WHERE id = 600"));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id = 500"));
            assertEquals(new BigDecimal("85.00"), scalarDecimal(connection,
                    "SELECT score FROM quiz_attempts WHERE id = 700"));

            assertEquals(1, scalarLong(connection,
                    "SELECT COUNT(*) FROM assessment_progress WHERE enrollment_id = 800 AND assessment_id = 500"));
            assertEquals("QUIZ", scalarString(connection,
                    "SELECT completion_source FROM assessment_progress WHERE enrollment_id = 800 AND assessment_id = 500"));
            assertEquals("2026-01-01 01:00:00", scalarString(connection,
                    "SELECT (completed_at AT TIME ZONE 'UTC')::text "
                            + "FROM assessment_progress WHERE enrollment_id = 800 AND assessment_id = 500"));

            // Expand phase compatibility: the current application can keep using
            // lesson_id, QUIZ lessons, and objective bullets until the contract release.
            assertEquals(300, scalarLong(connection,
                    "SELECT lesson_id FROM quizzes WHERE id = 500"));
            assertEquals(1, scalarLong(connection,
                    "SELECT COUNT(*) FROM lessons WHERE id = 300 AND content_type = 'QUIZ' AND deleted_at IS NULL"));
            assertEquals(1, scalarLong(connection,
                    "SELECT COUNT(*) FROM course_bullets WHERE id = 400 AND bullet_type = 'LEARNING_OBJECTIVE'"));
        }
    }

    private void migrateToVersion16() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("16"))
                .load()
                .migrate();
    }

    private Flyway migrationRunner() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("17"))
                .load();
    }

    private void seedLegacyCourse() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (id, full_name, email, role)
                    VALUES
                        (1, 'Legacy Instructor', 'instructor@example.test', 'USER'),
                        (2, 'Legacy Learner', 'learner@example.test', 'USER')
                    """);
            statement.executeUpdate("""
                    INSERT INTO courses (id, lecturer_id, title, slug, description, status)
                    VALUES (100, 1, 'Legacy course', 'legacy-course', 'Migration fixture', 'DRAFT')
                    """);
            statement.executeUpdate("""
                    INSERT INTO course_sections (id, course_id, title, position)
                    VALUES (200, 100, 'Legacy section', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO lessons (id, section_id, title, content_type, position)
                    VALUES
                        (300, 200, 'Legacy quiz lesson', 'QUIZ', 1),
                        (301, 200, 'Legacy article lesson', 'ARTICLE', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO course_bullets (id, course_id, bullet_type, content, position)
                    VALUES (400, 100, 'LEARNING_OBJECTIVE', 'Build a REST API', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO quizzes (id, lesson_id, title, passing_score, max_attempts)
                    VALUES (500, 300, 'Legacy quiz', 80.00, 3)
                    """);
            statement.executeUpdate("""
                    INSERT INTO quiz_questions (
                        id, quiz_id, question_text, question_type, options_json, points, position
                    ) VALUES (
                        600,
                        500,
                        'Which annotation declares a REST controller?',
                        'SINGLE_CHOICE',
                        '[{"text":"@RestController","isCorrect":true},{"text":"@Service","isCorrect":false}]'::jsonb,
                        1.00,
                        0
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO enrollments (id, student_id, course_id, status, progress)
                    VALUES (800, 2, 100, 'ACTIVE', 50.00)
                    """);
            statement.executeUpdate("""
                    INSERT INTO lesson_progress (
                        id, enrollment_id, lesson_id, completed_at, completion_source,
                        first_started_at, updated_at
                    ) VALUES (
                        900,
                        800,
                        300,
                        '2026-01-01T01:00:00Z',
                        'QUIZ',
                        '2026-01-01T00:55:00Z',
                        '2026-01-01T01:00:00Z'
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO quiz_attempts (
                        id, quiz_id, learner_id, answers_json, score, is_passed, submitted_at
                    ) VALUES (
                        700,
                        500,
                        2,
                        '{"600":[0]}'::jsonb,
                        85.00,
                        TRUE,
                        '2026-01-02T01:00:00Z'
                    )
                    """);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private long scalarLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String scalarString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private BigDecimal scalarDecimal(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getBigDecimal(1);
        }
    }
}
