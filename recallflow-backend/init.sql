-- RecallFlow MySQL Database Initialization Script

-- 1. Create Database
CREATE DATABASE IF NOT EXISTS recallflow_db;
USE recallflow_db;

-- Optional: Verify database is selected
SELECT DATABASE();

-- Note: When Spring Boot runs (`mvn spring-boot:run`),
-- Hibernate automatically creates all tables (users, topics, revision_logs, quizzes, etc.)
-- because spring.jpa.hibernate.ddl-auto=update is enabled.

-- Optional Sample Verification Queries (Run after starting Spring Boot):
-- SHOW TABLES;
-- SELECT * FROM users;
-- SELECT * FROM topics;
