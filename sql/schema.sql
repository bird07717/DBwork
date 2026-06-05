CREATE DATABASE IF NOT EXISTS bus_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE bus_agent;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS dispatch_advice;
DROP TABLE IF EXISTS ride_record;
DROP TABLE IF EXISTS bus_schedule;
DROP TABLE IF EXISTS driver;
DROP TABLE IF EXISTS bus_vehicle;
DROP TABLE IF EXISTS route_station;
DROP TABLE IF EXISTS station;
DROP TABLE IF EXISTS bus_route;
DROP TABLE IF EXISTS admin_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_admin_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bus_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_code VARCHAR(30) NOT NULL,
  route_name VARCHAR(100) NOT NULL,
  direction VARCHAR(50) NOT NULL,
  start_station_name VARCHAR(100) NOT NULL,
  end_station_name VARCHAR(100) NOT NULL,
  operation_start_time TIME NULL,
  operation_end_time TIME NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_route_code UNIQUE (route_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE station (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  station_code VARCHAR(30) NOT NULL,
  station_name VARCHAR(100) NOT NULL,
  area_name VARCHAR(100) NULL,
  longitude DECIMAL(10,6) NOT NULL,
  latitude DECIMAL(10,6) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_station_code UNIQUE (station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE route_station (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  station_id BIGINT NOT NULL,
  station_order INT NOT NULL,
  distance_from_start DECIMAL(8,2) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_route_station_order UNIQUE (route_id, station_order),
  CONSTRAINT uk_route_station UNIQUE (route_id, station_id),
  CONSTRAINT fk_route_station_route FOREIGN KEY (route_id) REFERENCES bus_route(id),
  CONSTRAINT fk_route_station_station FOREIGN KEY (station_id) REFERENCES station(id),
  INDEX idx_station_id (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bus_vehicle (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_code VARCHAR(30) NOT NULL,
  plate_no VARCHAR(20) NOT NULL,
  capacity INT NOT NULL DEFAULT 50,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_vehicle_code UNIQUE (vehicle_code),
  CONSTRAINT uk_vehicle_plate_no UNIQUE (plate_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE driver (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_no VARCHAR(30) NOT NULL,
  driver_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_driver_employee_no UNIQUE (employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bus_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  driver_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  depart_time DATETIME NOT NULL,
  period_type VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_schedule_route FOREIGN KEY (route_id) REFERENCES bus_route(id),
  CONSTRAINT fk_schedule_vehicle FOREIGN KEY (vehicle_id) REFERENCES bus_vehicle(id),
  CONSTRAINT fk_schedule_driver FOREIGN KEY (driver_id) REFERENCES driver(id),
  CONSTRAINT uk_vehicle_depart UNIQUE (vehicle_id, depart_time),
  CONSTRAINT uk_driver_depart UNIQUE (driver_id, depart_time),
  INDEX idx_schedule_route_date (route_id, schedule_date),
  INDEX idx_schedule_depart_time (depart_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ride_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  schedule_id BIGINT NULL,
  boarding_station_id BIGINT NOT NULL,
  alighting_station_id BIGINT NULL,
  ride_time DATETIME NOT NULL,
  period_type VARCHAR(20) NOT NULL,
  pay_type VARCHAR(20) NOT NULL DEFAULT 'mock',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ride_route FOREIGN KEY (route_id) REFERENCES bus_route(id),
  CONSTRAINT fk_ride_schedule FOREIGN KEY (schedule_id) REFERENCES bus_schedule(id),
  CONSTRAINT fk_ride_boarding_station FOREIGN KEY (boarding_station_id) REFERENCES station(id),
  CONSTRAINT fk_ride_alighting_station FOREIGN KEY (alighting_station_id) REFERENCES station(id),
  INDEX idx_ride_route_time (route_id, ride_time),
  INDEX idx_ride_boarding_time (boarding_station_id, ride_time),
  INDEX idx_ride_period (period_type, ride_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dispatch_advice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  period_type VARCHAR(20) NOT NULL,
  avg_load_rate DECIMAL(5,2) NULL,
  passenger_count INT NOT NULL DEFAULT 0,
  advice_level VARCHAR(20) NOT NULL,
  advice_content TEXT NOT NULL,
  ai_summary TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_dispatch_route FOREIGN KEY (route_id) REFERENCES bus_route(id),
  INDEX idx_advice_route_date (route_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_id BIGINT NULL,
  operation_type VARCHAR(50) NOT NULL,
  operation_content VARCHAR(255) NOT NULL,
  request_path VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_operation_admin FOREIGN KEY (admin_id) REFERENCES admin_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
