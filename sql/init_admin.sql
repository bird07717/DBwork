USE bus_agent;

INSERT INTO admin_user (
  username,
  password_hash,
  real_name,
  status,
  create_time,
  update_time
) VALUES (
  'wjhadmin',
  '616b36d689d7b76ea8ecc33ad287619c6bb121c2c00a49fdd32b52a52ff5567e',
  '王家豪',
  1,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  real_name = VALUES(real_name),
  status = VALUES(status),
  update_time = CURRENT_TIMESTAMP;
