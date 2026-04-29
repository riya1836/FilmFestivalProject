-- Film Festival Management System - RBAC Database Migration
-- Clean migration script with proper table drop order

-- ============================================
-- 1. CREATE USERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER', 'JURY') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 2. DROP DEPENDENT TABLES IN CORRECT ORDER
-- ============================================
DROP TABLE IF EXISTS evaluation;
DROP TABLE IF EXISTS jury_assignment;
DROP TABLE IF EXISTS jury;

-- ============================================
-- 3. CREATE JURY TABLE (FRESH)
-- ============================================
CREATE TABLE jury (
    jury_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_jury_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 4. CREATE JURY ASSIGNMENT TABLE
-- ============================================
CREATE TABLE jury_assignment (
    id INT PRIMARY KEY AUTO_INCREMENT,
    jury_id INT NOT NULL,
    film_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (jury_id) REFERENCES jury(jury_id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    UNIQUE KEY unique_jury_film (jury_id, film_id),
    INDEX idx_jury_id (jury_id),
    INDEX idx_film_id (film_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 5. CREATE EVALUATION TABLE
-- ============================================
CREATE TABLE evaluation (
    evaluation_id INT PRIMARY KEY AUTO_INCREMENT,
    jury_id INT NOT NULL,
    film_id INT NOT NULL,
    score INT NOT NULL CHECK (score >= 1 AND score <= 10),
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (jury_id) REFERENCES jury(jury_id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    UNIQUE KEY unique_jury_film_evaluation (jury_id, film_id),
    INDEX idx_jury_id (jury_id),
    INDEX idx_film_id (film_id),
    INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 6. CREATE AUDIT LOG TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS audit_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    action VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100),
    resource_id INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 7. ADD INDEXES TO EXISTING TABLES (OPTIONAL - may already exist)
-- ============================================
-- These indexes improve query performance
-- Comment out if you get duplicate index errors
-- ALTER TABLE film ADD INDEX idx_title (title);
-- ALTER TABLE film ADD INDEX idx_genre (genre);

-- ============================================
-- 8. INSERT TEST ADMIN USER
-- ============================================
-- Email: admin@filmfestival.com
-- Password: admin123 (pre-hashed with SHA-256)
INSERT INTO users (user_id, name, email, password_hash, role, is_active)
VALUES (1, 'System Admin', 'admin@filmfestival.com',
        'jCvZqIhlGT1SZvtTou6DbJG1QlqSshvyCpOA7N/bqNX/DgknwdtwD3/KrYTMY9Bc', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    is_active = VALUES(is_active);

-- ============================================
-- MIGRATION COMPLETE
-- ============================================
-- RBAC system successfully set up!
-- Default admin account:
--   Email: admin@filmfestival.com
--   Password: admin123
