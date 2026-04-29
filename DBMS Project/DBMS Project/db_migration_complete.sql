-- Complete Film Festival Database Migration with RBAC
-- This script creates all necessary tables and sets up RBAC

-- ============================================
-- 0. CREATE CORE TABLES (Film Festival System)
-- ============================================

-- VENUE TABLE
CREATE TABLE IF NOT EXISTS venue (
    venue_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FILM TABLE
CREATE TABLE IF NOT EXISTS film (
    film_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    director VARCHAR(255),
    genre VARCHAR(100),
    description TEXT,
    duration_minutes INT,
    release_year INT,
    country VARCHAR(100),
    language VARCHAR(100),
    rating DECIMAL(3, 1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_genre (genre),
    INDEX idx_director (director)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SCREENING TABLE
CREATE TABLE IF NOT EXISTS screening (
    screening_id INT PRIMARY KEY AUTO_INCREMENT,
    film_id INT NOT NULL,
    venue_id INT NOT NULL,
    screening_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    ticket_price DECIMAL(10,2) NOT NULL DEFAULT 10.00,
    seats_available INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (venue_id) REFERENCES venue(venue_id) ON DELETE CASCADE,
    INDEX idx_film_id (film_id),
    INDEX idx_venue_id (venue_id),
    INDEX idx_screening_date (screening_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ATTENDEE TABLE
CREATE TABLE IF NOT EXISTS attendee (
    attendee_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TICKET TABLE
CREATE TABLE IF NOT EXISTS ticket (
    ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    screening_id INT NOT NULL,
    attendee_id INT NOT NULL,
    seat_number VARCHAR(10),
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (screening_id) REFERENCES screening(screening_id) ON DELETE CASCADE,
    FOREIGN KEY (attendee_id) REFERENCES attendee(attendee_id) ON DELETE CASCADE,
    INDEX idx_screening_id (screening_id),
    INDEX idx_attendee_id (attendee_id),
    UNIQUE KEY unique_ticket (screening_id, seat_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AWARD TABLE
CREATE TABLE IF NOT EXISTS award (
    award_id INT PRIMARY KEY AUTO_INCREMENT,
    award_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_award_name (award_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FILM_AWARD TABLE (Junction table for films and awards)
CREATE TABLE IF NOT EXISTS film_award (
    id INT PRIMARY KEY AUTO_INCREMENT,
    film_id INT NOT NULL,
    award_id INT NOT NULL,
    year INT,
    won BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (award_id) REFERENCES award(award_id) ON DELETE CASCADE,
    UNIQUE KEY unique_film_award (film_id, award_id),
    INDEX idx_film_id (film_id),
    INDEX idx_award_id (award_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREW TABLE
CREATE TABLE IF NOT EXISTS crew (
    crew_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FILM_CREW TABLE (Junction table for films and crew)
CREATE TABLE IF NOT EXISTS film_crew (
    id INT PRIMARY KEY AUTO_INCREMENT,
    film_id INT NOT NULL,
    crew_id INT NOT NULL,
    role VARCHAR(100),
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (crew_id) REFERENCES crew(crew_id) ON DELETE CASCADE,
    INDEX idx_film_id (film_id),
    INDEX idx_crew_id (crew_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 1. CREATE USERS TABLE (RBAC)
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
-- 2. UPDATE ATTENDEE TABLE (Add user_id)
-- ============================================
ALTER TABLE attendee ADD COLUMN user_id INT UNIQUE DEFAULT NULL;
ALTER TABLE attendee ADD FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

-- ============================================
-- 3. CREATE JURY TABLE
-- ============================================
DROP TABLE IF EXISTS jury;

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
CREATE TABLE IF NOT EXISTS jury_assignment (
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
CREATE TABLE IF NOT EXISTS evaluation (
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
-- 7. INSERT TEST ADMIN USER
-- ============================================
-- Email: admin@filmfestival.com
-- Password: admin123 (pre-hashed with SHA-256 + salt + iterations)
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
