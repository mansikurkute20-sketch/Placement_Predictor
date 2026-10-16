-- ============================================================
-- PlacementPredictor - Full Database Setup Script
-- Run this in MySQL: mysql -u root -p < database_setup.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS placement_db;
USE placement_db;

-- ── Admins ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admins (
    admin_id   INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Students ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    branch     VARCHAR(20)  NOT NULL,
    cgpa       DECIMAL(4,2) NOT NULL,
    backlogs   INT          NOT NULL DEFAULT 0,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Companies ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS companies (
    company_id  INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    role         VARCHAR(100) NOT NULL,
    package_lpa  DECIMAL(5,2) NOT NULL,
    visit_date   DATE         NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Eligibility Criteria ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS eligibility_criteria (
    criteria_id  INT AUTO_INCREMENT PRIMARY KEY,
    company_id   INT          NOT NULL UNIQUE,
    min_cgpa     DECIMAL(4,2) NOT NULL DEFAULT 6.0,
    max_backlogs INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE
);

-- ── Allowed Branches ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS allowed_branches (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT         NOT NULL,
    branch     VARCHAR(20) NOT NULL,
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE,
    UNIQUE KEY uq_company_branch (company_id, branch)
);

-- ============================================================
-- Seed Data
-- ============================================================

-- Default admin (username: admin, password: admin123)
INSERT IGNORE INTO admins (username, password) VALUES ('admin', 'admin123');

-- Sample companies
INSERT IGNORE INTO companies (company_name, role, package_lpa, visit_date) VALUES
  ('TCS',       'Software Engineer',         3.50,  '2025-08-10'),
  ('Infosys',   'Systems Engineer',          3.80,  '2025-08-15'),
  ('Wipro',     'Project Engineer',          3.50,  '2025-08-20'),
  ('Cognizant', 'Programmer Analyst',        4.00,  '2025-09-01'),
  ('HCL',       'Graduate Engineer Trainee', 3.78,  '2025-09-05'),
  ('Google',    'Software Development Engineer', 25.00, '2025-09-20'),
  ('Microsoft', 'SDE Intern → FTE',         18.00, '2025-09-25');

-- Eligibility criteria per company
INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 6.00, 2 FROM companies WHERE company_name = 'TCS';

INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 6.50, 1 FROM companies WHERE company_name = 'Infosys';

INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 6.00, 2 FROM companies WHERE company_name = 'Wipro';

INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 6.50, 1 FROM companies WHERE company_name = 'Cognizant';

INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 6.50, 1 FROM companies WHERE company_name = 'HCL';

INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 8.50, 0 FROM companies WHERE company_name = 'Google';

INSERT IGNORE INTO eligibility_criteria (company_id, min_cgpa, max_backlogs)
SELECT company_id, 8.00, 0 FROM companies WHERE company_name = 'Microsoft';

-- Allowed branches
-- TCS, Infosys, Wipro, Cognizant, HCL → CS, IT, ME
INSERT IGNORE INTO allowed_branches (company_id, branch)
SELECT company_id, 'CS' FROM companies WHERE company_name IN ('TCS','Infosys','Wipro','Cognizant','HCL')
UNION ALL
SELECT company_id, 'IT' FROM companies WHERE company_name IN ('TCS','Infosys','Wipro','Cognizant','HCL')
UNION ALL
SELECT company_id, 'ME' FROM companies WHERE company_name IN ('TCS','Infosys','Wipro','Cognizant','HCL');

-- Google, Microsoft → CS, IT only
INSERT IGNORE INTO allowed_branches (company_id, branch)
SELECT company_id, 'CS' FROM companies WHERE company_name IN ('Google','Microsoft')
UNION ALL
SELECT company_id, 'IT' FROM companies WHERE company_name IN ('Google','Microsoft');

SELECT 'Database setup complete!' AS status;
