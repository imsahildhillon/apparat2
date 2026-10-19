-- Reset tables in dependency order.
DROP TABLE IF EXISTS booking_slots;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS users;


-- 1. Users
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       role VARCHAR(30) NOT NULL
);


-- 2. Resources
CREATE TABLE resources (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                           location VARCHAR(100) NOT NULL,
                           status VARCHAR(30) NOT NULL
);


-- 3. Bookings
CREATE TABLE bookings (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          resource_id INT NOT NULL,
                          start_at DATETIME NOT NULL,
                          end_at DATETIME NOT NULL,
                          status VARCHAR(20) NOT NULL,

                          CONSTRAINT fk_bookings_user
                              FOREIGN KEY (user_id) REFERENCES users(id),

                          CONSTRAINT fk_bookings_resource
                              FOREIGN KEY (resource_id) REFERENCES resources(id)
);


-- 4. Booking slots
CREATE TABLE booking_slots (
                               resource_id INT NOT NULL,
                               slot_start DATETIME NOT NULL,
                               booking_id INT NOT NULL,

                               PRIMARY KEY (resource_id, slot_start),

                               CONSTRAINT fk_slots_resource
                                   FOREIGN KEY (resource_id) REFERENCES resources(id),

                               CONSTRAINT fk_slots_booking
                                   FOREIGN KEY (booking_id) REFERENCES bookings(id)
);