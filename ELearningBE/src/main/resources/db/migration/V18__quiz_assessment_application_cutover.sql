-- Application cutover from lesson-owned quizzes to assessment-owned quizzes.
-- Legacy columns stay in place until the Task 07 contract migration, but new
-- assessment quizzes no longer need a synthetic lesson or a duplicated title.
ALTER TABLE quizzes
    ALTER COLUMN lesson_id DROP NOT NULL,
    ALTER COLUMN title DROP NOT NULL;

