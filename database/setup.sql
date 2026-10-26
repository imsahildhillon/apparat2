-- Apparat: builds the whole database from nothing.
--
-- WARNING: this DROPS and recreates every Apparat table in the database it
-- is run against. Never run it on a database that holds data you want to keep.
--
-- Choose the database on the command line, for example:
--   mysql -u root --port=3308 --protocol=TCP apparat2      < database/setup.sql
--   mysql -u root --port=3308 --protocol=TCP apparat2_test < database/setup.sql
-- (database/setup-db.sh creates both databases and runs this file for you.)

-- Remove old tables first, children before parents.
DROP TABLE IF EXISTS maintenance_windows;
DROP TABLE IF EXISTS booking_slots;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS users;


-- 1. Users. The password is stored only as a salted PBKDF2 hash.
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    role          VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL DEFAULT '',
    active        TINYINT(1)   NOT NULL DEFAULT 1
);


-- 2. Resources (instruments).
CREATE TABLE resources (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    status   VARCHAR(30)  NOT NULL
);


-- 3. Bookings.
CREATE TABLE bookings (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    resource_id INT NOT NULL,
    start_at    DATETIME NOT NULL,
    end_at      DATETIME NOT NULL,
    status      VARCHAR(20) NOT NULL,

    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_bookings_resource
        FOREIGN KEY (resource_id) REFERENCES resources(id)
);


-- 4. Booking slots: one row per 30-minute piece of a confirmed booking.
--    The primary key is what makes a double booking impossible: two
--    bookings can never own the same (instrument, slot start).
CREATE TABLE booking_slots (
    resource_id INT NOT NULL,
    slot_start  DATETIME NOT NULL,
    booking_id  INT NOT NULL,

    PRIMARY KEY (resource_id, slot_start),

    CONSTRAINT fk_slots_resource
        FOREIGN KEY (resource_id) REFERENCES resources(id),

    CONSTRAINT fk_slots_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id)
);


-- 5. Maintenance windows: times when an instrument cannot be booked.
CREATE TABLE maintenance_windows (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    resource_id INT NOT NULL,
    start_at    DATETIME NOT NULL,
    end_at      DATETIME NOT NULL,
    reason      VARCHAR(200) NOT NULL,
    created_by  INT NOT NULL,

    CONSTRAINT fk_maintenance_resource
        FOREIGN KEY (resource_id) REFERENCES resources(id),

    CONSTRAINT fk_maintenance_user
        FOREIGN KEY (created_by) REFERENCES users(id)
);


-- ---------------------------------------------------------------------
-- DEMO DATA for development, the review demo and the tests only.
-- These are not real users. The tests expect user 1 to be a student and
-- instrument 1 to exist and be ACTIVE, so keep the order below.
-- Passwords: Student@123, Faculty@123, Tech@123, Admin@123
-- ---------------------------------------------------------------------
INSERT INTO users (name, email, role, password_hash) VALUES
    ('Demo Student',    'student@apparat.test', 'STUDENT',    'pbkdf2$120000$yx+2Mm7fo6nSaF7KbFhB9A==$+yQCcPKNHbSTdTySDludNsc6vVTdM44FxfLsq3pjIfg='),
    ('Demo Faculty',    'faculty@apparat.test', 'FACULTY',    'pbkdf2$120000$mQte8y1aOlLLHOmg1AMN7g==$7QuBKBcVNBKAItZljZfedzn/LQirsGNURvtNpZ4XjDY='),
    ('Demo Technician', 'tech@apparat.test',    'TECHNICIAN', 'pbkdf2$120000$5YiXLQyX3ZuNXW3fUw22rA==$kJd+zghm5/hwIjSE4A+VdvvRPpjC6BX68k33epc0444='),
    ('Demo Admin',      'admin@apparat.test',   'ADMIN',      'pbkdf2$120000$w2IJ3AIr2tq7KKB0RL/G4w==$dkAnJPosl3vDMhiyt4IqA5pbsvdPKIUOllvQcWFBdUY=');

INSERT INTO resources (name, location, status) VALUES
    ('Electroscope',     'PRP-711', 'ACTIVE'),
    ('Prism',            'PRP-717', 'ACTIVE'),
    ('Vernier Calliper', 'PRP-717', 'ACTIVE'),
    ('Screw Gauge',      'PRP-717', 'ACTIVE');
