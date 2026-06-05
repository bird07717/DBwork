-- Ensure local backend processes can connect to MySQL with the application user.
CREATE USER IF NOT EXISTS 'bus_agent'@'localhost' IDENTIFIED BY 'bus_agent123';
GRANT ALL PRIVILEGES ON bus_agent.* TO 'bus_agent'@'localhost';
FLUSH PRIVILEGES;
